package com.avrix.mixin;

import com.avrix.core.KnotClassLoader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import test.game.TargetGameService;
import test.plugin.BasePluginService;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link CompositeClassProvider}.
 */
@DisplayName("CompositeClassProvider Unit Tests")
class CompositeClassProviderTest {

    private static final ClassLoader PARENT = CompositeClassProviderTest.class.getClassLoader();

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("Should reject a null KnotClassLoader")
    void shouldRejectNullClassLoader() {
        assertThatThrownBy(() -> new CompositeClassProvider(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("KnotClassLoader");
    }

    @Test
    @DisplayName("Should read bytecode from a KnotClassLoader URL")
    void shouldReadBytecodeThroughKnotClassLoader() throws Exception {
        byte[] expectedBytes = readClassBytes(TargetGameService.class);
        Path classesDir = Files.createDirectories(tempDir.resolve("classes"));
        writeBytes(classesDir.resolve("isolated").resolve("Fixture.class"), expectedBytes);

        try (KnotClassLoader loader = new KnotClassLoader(
                new URL[]{classesDir.toUri().toURL()}, PARENT)) {
            CompositeClassProvider provider = new CompositeClassProvider(loader);

            assertThat(provider.getClass("isolated.Fixture")).isEqualTo(expectedBytes);
        }
    }

    @Test
    @DisplayName("Should read JDK bytecode through the platform lookup")
    void shouldReadJdkBytecode() throws Exception {
        try (KnotClassLoader loader = new KnotClassLoader(new URL[0], PARENT)) {
            CompositeClassProvider provider = new CompositeClassProvider(loader);

            byte[] bytes = provider.getClass("java.lang.String");

            assertThat(bytes).isNotEmpty();
            assertThat(isClassFile(bytes)).isTrue();
        }
    }

    @Test
    @DisplayName("Should reject null and missing class names")
    void shouldRejectMissingClasses() throws Exception {
        try (KnotClassLoader loader = new KnotClassLoader(new URL[0], PARENT)) {
            CompositeClassProvider provider = new CompositeClassProvider(loader);

            assertThatThrownBy(() -> provider.getClass(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("Class name");

            assertThatThrownBy(() -> provider.getClass("missing.absent.AbsentClass"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Bytecode not found");
        }
    }

    @Test
    @DisplayName("Should return an empty map when no URLs are registered")
    void shouldReturnEmptyClassMapForEmptyClasspath() throws Exception {
        try (KnotClassLoader loader = new KnotClassLoader(new URL[0], PARENT)) {
            CompositeClassProvider provider = new CompositeClassProvider(loader);

            assertThat(provider.getAllClasses()).isEmpty();
        }
    }

    @Test
    @DisplayName("Should scan directory and JAR classpath entries and map their suppliers")
    void shouldScanDirectoryAndJarEntries() throws Exception {
        byte[] alphaBytes = readClassBytes(TargetGameService.class);
        byte[] betaBytes = readClassBytes(BasePluginService.class);

        Path classesDir = Files.createDirectories(tempDir.resolve("classes"));
        writeBytes(classesDir.resolve("scanned").resolve("sample").resolve("Alpha.class"), alphaBytes);
        writeBytes(classesDir.resolve("scanned").resolve("sample").resolve("Nested").resolve("Beta.class"), betaBytes);
        Files.writeString(classesDir.resolve("scanned").resolve("sample").resolve("notes.txt"), "ignored");

        Map<String, byte[]> jarEntries = new LinkedHashMap<>();
        jarEntries.put("META-INF/MANIFEST.MF", "Manifest-Version: 1.0\r\n\r\n".getBytes());
        jarEntries.put("jarred/sample/Gamma.class", alphaBytes);
        jarEntries.put("META-INF/versions/9/jarred/sample/Hidden.class", betaBytes);

        Path jarPath = tempDir.resolve("classes.jar");
        createJar(jarPath, jarEntries);

        try (KnotClassLoader loader = new KnotClassLoader(
                new URL[]{classesDir.toUri().toURL(), jarPath.toUri().toURL()}, PARENT)) {
            CompositeClassProvider provider = new CompositeClassProvider(loader);
            Map<String, Supplier<byte[]>> classes = provider.getAllClasses();

            assertThat(classes).containsKeys(
                    "scanned.sample.Alpha",
                    "scanned.sample.Nested.Beta",
                    "jarred.sample.Gamma");

            assertThat(classes.keySet()).noneMatch(className -> className.startsWith("META-INF"));

            assertThat(classes.get("scanned.sample.Alpha").get()).isEqualTo(alphaBytes);
            assertThat(classes.get("scanned.sample.Nested.Beta").get()).isEqualTo(betaBytes);
            assertThat(classes.get("jarred.sample.Gamma").get()).isEqualTo(alphaBytes);

            assertThatThrownBy(() -> classes.put("other.Placeholder", () -> new byte[0]))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    private static byte[] readClassBytes(Class<?> type) throws IOException {
        String resourcePath = type.getName().replace('.', '/').concat(".class");

        try (InputStream inputStream = PARENT.getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new IOException("Class resource not found: " + resourcePath);
            }
            return inputStream.readAllBytes();
        }
    }

    private static void writeBytes(Path target, byte[] bytes) throws IOException {
        Files.createDirectories(target.getParent());
        Files.write(target, bytes);
    }

    private static void createJar(Path jarPath, Map<String, byte[]> entries) throws IOException {
        try (JarOutputStream outputStream = new JarOutputStream(Files.newOutputStream(jarPath))) {
            for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
                outputStream.putNextEntry(new JarEntry(entry.getKey()));
                outputStream.write(entry.getValue());
                outputStream.closeEntry();
            }
        }
    }

    private static boolean isClassFile(byte[] bytes) {
        return bytes.length >= 4
                && (bytes[0] & 0xFF) == 0xCA
                && (bytes[1] & 0xFF) == 0xFE
                && (bytes[2] & 0xFF) == 0xBA
                && (bytes[3] & 0xFF) == 0xBE;
    }
}
