package com.avrix.core;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import test.game.TargetGameService;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Regression tests for loading classes from signed JARs through {@link KnotClassLoader}.
 */
@DisplayName("KnotClassLoader signed JAR reading")
class KnotClassLoaderSignedJarTest {

    private static final ClassLoader PARENT = ClassLoader.getPlatformClassLoader();
    private static final String PASSWORD = "changeit";
    private static final String ALIAS = "avrixkey";

    @TempDir
    private Path tempDir;

    @Test
    @DisplayName("Should load a valid signed JAR and reject a tampered class entry")
    void shouldLoadSignedJarAndRejectTamperedClassEntry() throws Exception {
        Path jarsigner = findJdkTool("jarsigner");
        Path keytool = findJdkTool("keytool");
        Assumptions.assumeTrue(jarsigner != null && keytool != null, "JDK signing tools are unavailable");

        Path jar = createClassJar("signed.jar");
        Path keystore = tempDir.resolve("keystore.p12");
        runTool(keytool, List.of(
                "-genkeypair",
                "-alias", ALIAS,
                "-keyalg", "RSA",
                "-keysize", "2048",
                "-validity", "365",
                "-dname", "CN=Avrix Test",
                "-keystore", keystore.toString(),
                "-storetype", "PKCS12",
                "-storepass", PASSWORD,
                "-keypass", PASSWORD
        ));
        runTool(jarsigner, List.of(
                "-keystore", keystore.toString(),
                "-storepass", PASSWORD,
                "-keypass", PASSWORD,
                "-sigalg", "SHA256withRSA",
                "-digestalg", "SHA-256",
                jar.toString(),
                ALIAS
        ));

        verifySignedFixture(jar);

        try (KnotClassLoader loader = new KnotClassLoader(new URL[]{jar.toUri().toURL()}, PARENT)) {
            Class<?> loaded = loader.loadClass(TargetGameService.class.getName());
            Object instance = loaded.getDeclaredConstructor().newInstance();

            assertThat(loaded.getClassLoader()).isSameAs(loader);
            assertThat(loaded.getMethod("getGreeting", String.class).invoke(instance, "World"))
                    .isEqualTo("Hello, World");
        }

        String classPath = classResourcePath(TargetGameService.class);
        byte[] originalBytes = readClassBytes(TargetGameService.class);
        byte[] tamperedBytes = Arrays.copyOf(originalBytes, originalBytes.length + 1);
        replaceEntryBytes(jar, classPath, tamperedBytes);

        try (KnotClassLoader loader = new KnotClassLoader(new URL[]{jar.toUri().toURL()}, PARENT)) {
            assertThatThrownBy(() -> loader.loadClass(TargetGameService.class.getName()))
                    .isInstanceOf(SecurityException.class);
        }
    }

    private void verifySignedFixture(Path jar) throws IOException {
        // Verify the fixture with the JDK so a malformed signature cannot mask loader regressions.
        try (JarFile signedJar = new JarFile(jar.toFile(), true)) {
            List<String> entryNames = Collections.list(signedJar.entries()).stream()
                    .map(JarEntry::getName)
                    .toList();
            assertThat(entryNames).anyMatch(name -> name.startsWith("META-INF/") && name.endsWith(".SF"));
            assertThat(entryNames).anyMatch(name -> name.startsWith("META-INF/") && name.endsWith(".RSA"));

            JarEntry classEntry = signedJar.getJarEntry(classResourcePath(TargetGameService.class));
            assertThat(classEntry).isNotNull();
            try (InputStream stream = signedJar.getInputStream(classEntry)) {
                assertThat(stream.readAllBytes()).isNotEmpty();
            }
        }
    }

    private Path createClassJar(String name) throws IOException {
        Path jar = tempDir.resolve(name);
        String resourcePath = classResourcePath(TargetGameService.class);
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");

        try (JarOutputStream output = new JarOutputStream(Files.newOutputStream(jar), manifest)) {
            output.putNextEntry(new JarEntry(resourcePath));
            output.write(readClassBytes(TargetGameService.class));
            output.closeEntry();
        }
        return jar;
    }

    private static void replaceEntryBytes(Path jar, String entryName, byte[] replacement) throws IOException {
        Path rewritten = jar.resolveSibling(jar.getFileName() + ".rewritten");
        try (JarFile source = new JarFile(jar.toFile());
                JarOutputStream output = new JarOutputStream(Files.newOutputStream(rewritten))) {
            Enumeration<JarEntry> entries = source.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                output.putNextEntry(new JarEntry(entry.getName()));
                if (!entry.isDirectory()) {
                    byte[] bytes;
                    try (InputStream stream = source.getInputStream(entry)) {
                        bytes = stream.readAllBytes();
                    }
                    output.write(entryName.equals(entry.getName()) ? replacement : bytes);
                }
                output.closeEntry();
            }
        }
        Files.move(rewritten, jar, StandardCopyOption.REPLACE_EXISTING);
    }

    private static byte[] readClassBytes(Class<?> type) throws IOException {
        String resourcePath = classResourcePath(type);
        try (InputStream stream = type.getClassLoader().getResourceAsStream(resourcePath)) {
            if (stream == null) {
                throw new IOException("Class resource not found: " + resourcePath);
            }
            return stream.readAllBytes();
        }
    }

    private static String classResourcePath(Class<?> type) {
        return type.getName().replace('.', '/') + ".class";
    }

    private static Path findJdkTool(String name) {
        String executableName = System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win")
                ? name + ".exe"
                : name;
        Path executable = Path.of(System.getProperty("java.home"), "bin", executableName);
        return Files.isRegularFile(executable) ? executable : null;
    }

    private void runTool(Path executable, List<String> arguments) throws IOException, InterruptedException {
        List<String> command = new ArrayList<>();
        command.add(executable.toString());
        command.addAll(arguments);
        Path output = Files.createTempFile(tempDir, "signing-tool-", ".log");

        try {
            Process process = new ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .redirectOutput(output.toFile())
                    .start();
            try {
                boolean finished = process.waitFor(60, TimeUnit.SECONDS);
                String log = Files.readString(output, StandardCharsets.UTF_8);
                assertThat(finished).as("Timed out running %s:%n%s", command, log).isTrue();
                assertThat(process.exitValue()).as("Command %s failed:%n%s", command, log).isZero();
            } finally {
                process.destroyForcibly();
            }
        } finally {
            Files.deleteIfExists(output);
        }
    }
}
