package com.avrix.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import test.game.TargetGameService;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link KnotClassLoader} classloading, URL, resource, and native library behavior.
 */
@DisplayName("KnotClassLoader Unit Tests")
class KnotClassLoaderTest {

    private static final ClassLoader PARENT = KnotClassLoaderTest.class.getClassLoader();

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("Should construct with the default classloading policy")
    void shouldConstructWithDefaultPolicy() throws Exception {
        try (KnotClassLoader loader = new KnotClassLoader(new URL[0], PARENT)) {
            assertThat(loader.getParent()).isSameAs(PARENT);
            assertThat(loader.getURLs()).isEmpty();
        }
    }

    @Test
    @DisplayName("Should construct with a custom classloading policy")
    void shouldConstructWithCustomPolicy() throws Exception {
        Path classesDir = Files.createDirectories(tempDir.resolve("classes"));
        writeClassFile(classesDir, TargetGameService.class.getName());

        ClassLoadingPolicy policy = ClassLoadingPolicy.builder()
                .delegateToParent("test.game.")
                .build();

        try (KnotClassLoader loader = new KnotClassLoader(
                new URL[]{classesDir.toUri().toURL()}, PARENT, policy)) {
            Class<?> loaded = loader.loadClass(TargetGameService.class.getName());

            assertThat(loaded).isSameAs(TargetGameService.class);
            assertThat(loaded.getClassLoader()).isNotSameAs(loader);
        }
    }

    @Test
    @DisplayName("Should reject null constructor arguments")
    void shouldRejectNullConstructorArguments() {
        assertThatThrownBy(() -> new KnotClassLoader(null, PARENT))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("URLs");

        assertThatThrownBy(() -> new KnotClassLoader(new URL[0], null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Parent");

        assertThatThrownBy(() -> new KnotClassLoader(new URL[0], PARENT, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("ClassLoadingPolicy");
    }

    @Test
    @DisplayName("Should delegate JVM, logging, and Avrix classes to the parent loader")
    void shouldDelegateProtectedPrefixesToParent() throws Exception {
        try (KnotClassLoader loader = new KnotClassLoader(new URL[0], PARENT)) {
            assertThat(loader.loadClass("java.lang.String")).isSameAs(String.class);
            assertThat(loader.loadClass("org.slf4j.Logger")).isSameAs(Logger.class);
            assertThat(loader.loadClass("com.avrix.core.ClassTransformer")).isSameAs(ClassTransformer.class);
        }
    }

    @Test
    @DisplayName("Should resolve a non-delegated test class locally from a registered URL")
    void shouldResolveTestClassLocally() throws Exception {
        Path classesDir = Files.createDirectories(tempDir.resolve("classes"));
        writeClassFile(classesDir, TargetGameService.class.getName());

        try (KnotClassLoader loader = new KnotClassLoader(
                new URL[]{classesDir.toUri().toURL()}, PARENT)) {
            Class<?> loaded = loader.loadClass(TargetGameService.class.getName());

            assertThat(loaded.getClassLoader()).isSameAs(loader);
            assertThat(loaded).isNotSameAs(TargetGameService.class);
        }
    }

    @Test
    @DisplayName("Should return the same class instance when a class is requested twice")
    void shouldReturnAlreadyLoadedClass() throws Exception {
        Path classesDir = Files.createDirectories(tempDir.resolve("classes"));
        writeClassFile(classesDir, TargetGameService.class.getName());

        try (KnotClassLoader loader = new KnotClassLoader(
                new URL[]{classesDir.toUri().toURL()}, PARENT)) {
            Class<?> first = loader.loadClass(TargetGameService.class.getName());
            Class<?> second = loader.loadClass(TargetGameService.class.getName());

            assertThat(second).isSameAs(first);
        }
    }

    @Test
    @DisplayName("Should fall back to the parent loader when no local class resource exists")
    void shouldFallBackToParent() throws Exception {
        try (KnotClassLoader loader = new KnotClassLoader(new URL[0], PARENT)) {
            Class<?> loaded = loader.loadClass(TargetGameService.class.getName());

            assertThat(loaded).isSameAs(TargetGameService.class);
            assertThat(loaded.getClassLoader()).isSameAs(PARENT);
        }
    }

    @Test
    @DisplayName("Should throw ClassNotFoundException for a class missing locally and in the parent")
    void shouldThrowForUnknownClass() throws Exception {
        try (KnotClassLoader loader = new KnotClassLoader(new URL[0], PARENT)) {
            assertThatThrownBy(() -> loader.loadClass("com.avrix.missing.DoesNotExist"))
                    .isInstanceOf(ClassNotFoundException.class)
                    .hasMessageContaining("com.avrix.missing.DoesNotExist");
        }
    }

    @Test
    @DisplayName("Should throw ClassNotFoundException from findClass when no local resource exists")
    void shouldThrowFromFindClassForMissingClass() throws Exception {
        try (KnotClassLoader loader = new KnotClassLoader(new URL[0], PARENT)) {
            assertThatThrownBy(() -> loader.findClass("missing.local.AbsentClass"))
                    .isInstanceOf(ClassNotFoundException.class)
                    .hasMessageContaining("missing.local.AbsentClass");
        }
    }

    @Test
    @DisplayName("Should deduplicate equivalent URLs across constructor and addURL")
    void shouldDeduplicateUrls() throws Exception {
        Path classesDir = Files.createDirectories(tempDir.resolve("classes"));
        URL url = classesDir.toUri().toURL();

        try (KnotClassLoader loader = new KnotClassLoader(new URL[]{url, url}, PARENT)) {
            assertThat(loader.getURLs()).hasSize(1);

            loader.addURL(url);

            assertThat(loader.getURLs()).hasSize(1);
        }
    }

    @Test
    @DisplayName("Should reject null and malformed URLs in addURL")
    void shouldRejectInvalidUrls() throws Exception {
        try (KnotClassLoader loader = new KnotClassLoader(new URL[0], PARENT)) {
            assertThatThrownBy(() -> loader.addURL(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("URL");

            URL malformedUrl = new URL("http", "example.com", "/invalid path.txt");

            assertThatThrownBy(() -> loader.addURL(malformedUrl))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid URI syntax");
        }
    }

    @Test
    @DisplayName("Should validate native path registration")
    void shouldValidateNativePaths() throws Exception {
        Path missing = tempDir.resolve("missing-native-dir");
        Path regularFile = Files.writeString(tempDir.resolve("not-a-directory.txt"), "content");

        try (KnotClassLoader loader = new KnotClassLoader(new URL[0], PARENT)) {
            assertThatThrownBy(() -> loader.addNativePath(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("Native path");

            assertThatThrownBy(() -> loader.addNativePath(missing))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("existing directory");

            assertThatThrownBy(() -> loader.addNativePath(regularFile))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("existing directory");

            assertThatThrownBy(() -> loader.addNativePaths(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("Paths");
        }
    }

    @Test
    @DisplayName("Should resolve a native library as an absolute path")
    void shouldResolveNativeLibrary() throws Exception {
        Path nativeDir = Files.createDirectories(tempDir.resolve("native"));
        Path libraryFile = Files.writeString(nativeDir.resolve("dummy.dll"), "stub");

        TestableKnotClassLoader loader = new TestableKnotClassLoader(new URL[0], PARENT);
        try (loader) {
            loader.addNativePath(nativeDir);

            String resolved = loader.resolveLibrary("dummy");

            assertThat(resolved).isNotNull();
            assertThat(Path.of(resolved))
                    .isAbsolute()
                    .isEqualTo(libraryFile.toAbsolutePath().normalize());
        }
    }

    @Test
    @DisplayName("Should resolve a native library with a platform-independent extension")
    void shouldResolveNativeLibraryByExtensionFallback() throws Exception {
        Path nativeDir = Files.createDirectories(tempDir.resolve("native"));
        Path libraryFile = Files.writeString(nativeDir.resolve("nativefoo.so"), "stub");

        TestableKnotClassLoader loader = new TestableKnotClassLoader(new URL[0], PARENT);
        try (loader) {
            loader.addNativePath(nativeDir);

            String resolved = loader.resolveLibrary("nativefoo");

            assertThat(resolved).isNotNull();
            assertThat(Path.of(resolved)).isEqualTo(libraryFile.toAbsolutePath().normalize());
        }
    }

    @Test
    @DisplayName("Should return null when a native library cannot be resolved")
    void shouldReturnNullForMissingNativeLibrary() throws Exception {
        TestableKnotClassLoader loader = new TestableKnotClassLoader(new URL[0], PARENT);
        try (loader) {
            assertThat(loader.resolveLibrary("avrix_missing_native_library_12345")).isNull();
        }
    }

    @Test
    @DisplayName("Should register multiple native paths and search all of them")
    void shouldRegisterMultipleNativePaths() throws Exception {
        Path firstDir = Files.createDirectories(tempDir.resolve("native-first"));
        Path secondDir = Files.createDirectories(tempDir.resolve("native-second"));
        Path libraryFile = Files.writeString(secondDir.resolve("multi.dll"), "stub");

        TestableKnotClassLoader loader = new TestableKnotClassLoader(new URL[0], PARENT);
        try (loader) {
            loader.addNativePaths(List.of(firstDir, secondDir));

            String resolved = loader.resolveLibrary("multi");

            assertThat(resolved).isNotNull();
            assertThat(Path.of(resolved)).isEqualTo(libraryFile.toAbsolutePath().normalize());
        }
    }

    @Test
    @DisplayName("Should invoke a configured ClassTransformer while loading a local class")
    void shouldInvokeConfiguredTransformer() throws Exception {
        Path classesDir = Files.createDirectories(tempDir.resolve("classes"));
        writeClassFile(classesDir, TargetGameService.class.getName());

        List<String> transformedClasses = new ArrayList<>();

        try (KnotClassLoader loader = new KnotClassLoader(
                new URL[]{classesDir.toUri().toURL()}, PARENT)) {
            loader.setClassTransformer((className, originalBytes) -> {
                transformedClasses.add(className);
                return null;
            });

            Class<?> loaded = loader.loadClass(TargetGameService.class.getName());

            assertThat(loaded.getClassLoader()).isSameAs(loader);
            assertThat(transformedClasses).containsExactly(TargetGameService.class.getName());
        }
    }

    @Test
    @DisplayName("Should still define a class when the transformer fails")
    void shouldIgnoreTransformerFailure() throws Exception {
        Path classesDir = Files.createDirectories(tempDir.resolve("classes"));
        writeClassFile(classesDir, TargetGameService.class.getName());

        try (KnotClassLoader loader = new KnotClassLoader(
                new URL[]{classesDir.toUri().toURL()}, PARENT)) {
            loader.setClassTransformer((className, originalBytes) -> {
                throw new IllegalStateException("Transformer failure");
            });

            Class<?> loaded = loader.loadClass(TargetGameService.class.getName());

            assertThat(loaded.getClassLoader()).isSameAs(loader);
        }
    }

    @Test
    @DisplayName("Should find local resources and tolerate duplicate resource paths")
    void shouldHandleResourceCollisions() throws Exception {
        Path firstDir = Files.createDirectories(tempDir.resolve("first"));
        Path secondDir = Files.createDirectories(tempDir.resolve("second"));
        Files.createDirectories(firstDir.resolve("dup"));
        Files.createDirectories(secondDir.resolve("dup"));
        Files.writeString(firstDir.resolve("dup").resolve("data.txt"), "first");
        Files.writeString(secondDir.resolve("dup").resolve("data.txt"), "second");

        try (KnotClassLoader loader = new KnotClassLoader(
                new URL[]{firstDir.toUri().toURL(), secondDir.toUri().toURL()}, PARENT)) {
            assertThat(loader.findResource("dup/data.txt")).isNotNull();
            assertThat(loader.findResource("dup/data.txt")).isNotNull();
            assertThat(loader.getResource("dup/data.txt")).isNotNull();
            assertThat(Collections.list(loader.findResources("dup/data.txt"))).hasSize(2);
            assertThat(loader.findResource("dup/missing.txt")).isNull();
        }
    }

    @Test
    @DisplayName("Should close idempotently")
    void shouldCloseIdempotently() throws Exception {
        KnotClassLoader loader = new KnotClassLoader(new URL[0], PARENT);

        loader.close();
        loader.close();

        assertThat(loader.getURLs()).isEmpty();
    }

    private static void writeClassFile(Path root, String className) throws IOException {
        String resourcePath = className.replace('.', '/').concat(".class");
        Path target = root.resolve(resourcePath.replace('/', java.io.File.separatorChar));
        Files.createDirectories(target.getParent());

        try (InputStream inputStream = PARENT.getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new IOException("Class resource not found: " + resourcePath);
            }
            Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /**
     * Test subclass exposing the protected native library resolution hook.
     */
    private static final class TestableKnotClassLoader extends KnotClassLoader {

        private TestableKnotClassLoader(URL[] urls, ClassLoader parent) {
            super(urls, parent);
        }

        private String resolveLibrary(String libName) {
            return findLibrary(libName);
        }
    }
}
