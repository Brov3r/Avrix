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
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Edge-case tests for {@link CompositeClassProvider} lookup fallbacks.
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
    @DisplayName("Should surface a class-not-found error when every lookup path misses")
    void shouldFailWhenEveryResourceLookupMisses() throws Exception {
        try (KnotClassLoader loader = new ThrowingResourceKnotClassLoader(
                new URL[0], new NoResourceParentClassLoader())) {
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
    @DisplayName("Should report a missing class when the active loader is absent")
    void shouldFailWithoutActiveLoader() {
        CompositeClassProvider provider = new CompositeClassProvider(() -> null);

        assertThatThrownBy(() -> provider.getClass("test.game.TargetGameService"))
                .isInstanceOf(ClassNotFoundException.class)
                .hasMessageContaining("No active class loader");
    }

    @Test
    @DisplayName("Should expose a supplier-backed provider")
    void shouldUseSupplierBackedLoader() throws Exception {
        try (KnotClassLoader loader = new KnotClassLoader(new URL[0], PARENT)) {
            CompositeClassProvider provider = new CompositeClassProvider(() -> loader);

            assertThat(provider.getClassNode("java.lang.Object").name).isEqualTo("java/lang/Object");
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
