package com.avrix.mixin;

import com.avrix.core.KnotClassLoader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import test.game.TargetGameService;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
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
 * Edge-case tests for {@link CompositeClassProvider} classpath scanning and lookup fallbacks.
 * <p>
 * These tests complement {@link CompositeClassProviderTest} by exercising scheme branches, corrupt
 * archives, and resource-lookup fallbacks that the happy-path tests do not reach.
 */
@DisplayName("CompositeClassProvider Edge Case Tests")
class CompositeClassProviderEdgeTest {

    private static final ClassLoader PARENT = CompositeClassProviderEdgeTest.class.getClassLoader();

    @TempDir
    Path tempDir;

    private boolean originalJarUseCaches;

    @BeforeEach
    void setUp() {
        // JarURLConnection caches open archives by default, which keeps file locks on Windows.
        originalJarUseCaches = URLConnection.getDefaultUseCaches("jar");
        URLConnection.setDefaultUseCaches("jar", false);
    }

    @AfterEach
    void tearDown() {
        URLConnection.setDefaultUseCaches("jar", originalJarUseCaches);
    }

    @Test
    @DisplayName("Should fall back to the platform class loader when the knot loader misses")
    void shouldFallBackToPlatformClassLoader() throws Exception {
        try (KnotClassLoader loader = new KnotClassLoader(new URL[0], new NoResourceParentClassLoader())) {
            CompositeClassProvider provider = new CompositeClassProvider(loader);

            byte[] bytes = provider.getClass("java.lang.String");

            assertThat(bytes).isNotEmpty();
            assertThat(isClassFile(bytes)).isTrue();
        }
    }

    @Test
    @DisplayName("Should surface an illegal argument when every lookup path misses")
    void shouldFailWhenEveryResourceLookupMisses() throws Exception {
        try (KnotClassLoader loader = new ThrowingResourceKnotClassLoader(new URL[0], new NoResourceParentClassLoader())) {
            CompositeClassProvider provider = new CompositeClassProvider(loader);

            assertThatThrownBy(() -> provider.getClass("missing.edge.AbsentClass"))
                    .isInstanceOf(ClassNotFoundException.class)
                    .hasMessageContaining("Bytecode not found");
        }
    }

    @Test
    @DisplayName("Should use the system resource stream when earlier lookups miss")
    void shouldFallBackToSystemResourceStream() throws Exception {
        String resourcePath = "test/game/TargetGameService.class";
        Assumptions.assumeTrue(
                ClassLoader.getSystemResource(resourcePath) != null,
                "The system class loader does not expose the test classpath in this environment"
        );

        try (KnotClassLoader loader = new KnotClassLoader(new URL[0], new NoResourceParentClassLoader())) {
            CompositeClassProvider provider = new CompositeClassProvider(loader);

            assertThat(provider.getClass("test.game.TargetGameService"))
                    .isEqualTo(readClassBytes(TargetGameService.class));
        }
    }

    @Test
    @DisplayName("Should ignore file URLs that are neither directories nor JAR archives")
    void shouldIgnoreNonDirectoryAndNonJarFileUrls() throws Exception {
        Path textFile = tempDir.resolve("notes.txt");
        Files.writeString(textFile, "not a classpath entry");
        Path missingFile = tempDir.resolve("missing.jar");

        try (KnotClassLoader loader = new KnotClassLoader(
                new URL[]{textFile.toUri().toURL(), missingFile.toUri().toURL()}, PARENT)) {
            CompositeClassProvider provider = new CompositeClassProvider(loader);

            assertThat(provider.getAllClasses()).isEmpty();
        }
    }

    @Test
    @DisplayName("Should ignore non-file URLs without attempting to open them")
    void shouldIgnoreNonFileUrls() throws Exception {
        URL httpUrl = URI.create("http://example.invalid/avrix-classes/").toURL();

        try (KnotClassLoader loader = new KnotClassLoader(new URL[]{httpUrl}, PARENT)) {
            CompositeClassProvider provider = new CompositeClassProvider(loader);

            assertThat(provider.getAllClasses()).isEmpty();
        }
    }

    @Test
    @DisplayName("Should survive a file with a .jar extension that is not a valid archive")
    void shouldSurviveCorruptJarFile() throws Exception {
        Path corruptJar = tempDir.resolve("corrupt.jar");
        Files.writeString(corruptJar, "this is definitely not a zip archive");

        try (KnotClassLoader loader = new KnotClassLoader(new URL[]{corruptJar.toUri().toURL()}, PARENT)) {
            CompositeClassProvider provider = new CompositeClassProvider(loader);

            assertThat(provider.getAllClasses()).isEmpty();
        }
    }

    @Test
    @DisplayName("Should scan jar-scheme URLs and skip directories, META-INF, and non-class entries")
    void shouldScanJarSchemeUrlAndSkipNonClassEntries() throws Exception {
        byte[] classBytes = readClassBytes(TargetGameService.class);

        Map<String, byte[]> entries = new LinkedHashMap<>();
        entries.put("META-INF/MANIFEST.MF", "Manifest-Version: 1.0\r\n\r\n".getBytes());
        entries.put("META-INF/versions/9/hidden/Hidden.class", classBytes);
        entries.put("dir/", new byte[0]);
        entries.put("dir/notes.txt", "ignored".getBytes());
        entries.put("dir/Visible.class", classBytes);

        Path jarPath = tempDir.resolve("jar-scheme.jar");
        createJar(jarPath, entries);

        URL jarUrl = URI.create("jar:" + jarPath.toUri().toASCIIString() + "!/").toURL();

        try (KnotClassLoader loader = new KnotClassLoader(new URL[]{jarUrl}, PARENT)) {
            CompositeClassProvider provider = new CompositeClassProvider(loader);
            Map<String, Supplier<byte[]>> classes = provider.getAllClasses();

            assertThat(classes).containsOnlyKeys("dir.Visible");
            assertThat(classes.get("dir.Visible").get()).isEqualTo(classBytes);
        }
    }

    @Test
    @DisplayName("Should ignore a jar-scheme URL whose archive does not exist")
    void shouldIgnoreMissingJarSchemeUrl() throws Exception {
        Path missingJar = tempDir.resolve("absent.jar");
        URL jarUrl = URI.create("jar:" + missingJar.toUri().toASCIIString() + "!/").toURL();

        try (KnotClassLoader loader = new KnotClassLoader(new URL[]{jarUrl}, PARENT)) {
            CompositeClassProvider provider = new CompositeClassProvider(loader);

            assertThat(provider.getAllClasses()).isEmpty();
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

    /**
     * Parent loader that never exposes resources so the knot lookup falls through to later stages.
     */
    private static final class NoResourceParentClassLoader extends ClassLoader {

        NoResourceParentClassLoader() {
            super(ClassLoader.getPlatformClassLoader());
        }

        @Override
        public URL getResource(String name) {
            return null;
        }
    }

    /**
     * Knot loader whose resource lookup fails, forcing the composite provider to handle a read error.
     */
    private static final class ThrowingResourceKnotClassLoader extends KnotClassLoader {

        ThrowingResourceKnotClassLoader(URL[] urls, ClassLoader parent) {
            super(urls, parent);
        }

        @Override
        public InputStream getResourceAsStream(String name) {
            throw new IllegalStateException("Simulated resource lookup failure");
        }
    }
}
