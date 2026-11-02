package com.avrix.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import test.game.TargetGameService;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Edge-case tests for {@link KnotClassLoader} resource, native library, class definition, and URL handling.
 */
@DisplayName("KnotClassLoader Edge Case Tests")
class KnotClassLoaderEdgeTest {

    private static final ClassLoader PARENT = KnotClassLoaderEdgeTest.class.getClassLoader();

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("Should ignore duplicate native path registrations")
    void shouldIgnoreDuplicateNativePathRegistrations() throws Exception {
        Path nativeDir = Files.createDirectories(tempDir.resolve("native"));
        Path library = Files.writeString(nativeDir.resolve("dupe.dll"), "stub");

        try (TestableKnotClassLoader loader = new TestableKnotClassLoader(new URL[0], PARENT)) {
            loader.addNativePath(nativeDir);
            loader.addNativePath(nativeDir);

            String resolved = loader.resolveLibrary("dupe");

            assertThat(resolved).isNotNull();
            assertThat(Path.of(resolved)).isEqualTo(library.toAbsolutePath().normalize());
        }
    }

    @Test
    @DisplayName("Should scan every candidate and return null when no native file matches")
    void shouldScanAllCandidatesWhenNoNativeFileMatches() throws Exception {
        Path nativeDir = Files.createDirectories(tempDir.resolve("native-empty"));
        Files.writeString(nativeDir.resolve("unrelated.txt"), "stub");

        try (TestableKnotClassLoader loader = new TestableKnotClassLoader(new URL[0], PARENT)) {
            loader.addNativePath(nativeDir);

            assertThat(loader.resolveLibrary("absentlib")).isNull();
        }
    }

    @Test
    @DisplayName("Should resolve a lib-prefixed macOS style native library")
    void shouldResolveLibPrefixedNativeLibrary() throws Exception {
        Path nativeDir = Files.createDirectories(tempDir.resolve("native-lib"));
        Path library = Files.writeString(nativeDir.resolve("libedge.dylib"), "stub");

        try (TestableKnotClassLoader loader = new TestableKnotClassLoader(new URL[0], PARENT)) {
            loader.addNativePath(nativeDir);

            String resolved = loader.resolveLibrary("edge");

            assertThat(resolved).isNotNull();
            assertThat(Path.of(resolved)).isEqualTo(library.toAbsolutePath().normalize());
        }
    }

    @Test
    @DisplayName("Should fall back to local resolution when the parent cannot load a delegated class")
    void shouldFallBackWhenParentDelegationFails() throws Exception {
        Path classesDir = Files.createDirectories(tempDir.resolve("classes"));
        writeClassFile(classesDir, TargetGameService.class.getName());

        ClassLoadingPolicy policy = ClassLoadingPolicy.builder()
                .delegateToParent("test.game.")
                .build();

        try (TestableKnotClassLoader loader = new TestableKnotClassLoader(
                new URL[]{classesDir.toUri().toURL()},
                new SelectiveParent(TargetGameService.class.getName(), PARENT),
                policy)) {
            Class<?> loaded = loader.loadClass(TargetGameService.class.getName());

            assertThat(loaded.getClassLoader()).isSameAs(loader);
        }
    }

    @Test
    @DisplayName("Should resolve a locally defined class when the resolve flag is set")
    void shouldResolveLocallyDefinedClass() throws Exception {
        Path classesDir = Files.createDirectories(tempDir.resolve("classes"));
        writeClassFile(classesDir, TargetGameService.class.getName());

        try (TestableKnotClassLoader loader = new TestableKnotClassLoader(
                new URL[]{classesDir.toUri().toURL()}, PARENT)) {
            Class<?> loaded = loader.loadResolved(TargetGameService.class.getName());

            assertThat(loaded.getClassLoader()).isSameAs(loader);
        }
    }

    @Test
    @DisplayName("Should wrap resource read failures in ClassNotFoundException")
    void shouldWrapResourceReadFailure() throws Exception {
        try (TestableKnotClassLoader loader = new TestableKnotClassLoader(new URL[0], PARENT)) {
            loader.resourceOverride = failingReadUrl();

            assertThatThrownBy(() -> loader.findClassDirect("fake.Broken"))
                    .isInstanceOf(ClassNotFoundException.class)
                    .hasMessageContaining("Failed to read bytecode for: fake.Broken")
                    .hasCauseInstanceOf(IOException.class);
        }
    }

    @Test
    @DisplayName("Should keep the resource URL as code source when the connection cannot be reopened")
    void shouldFallBackWhenCodeSourceConnectionFails() throws Exception {
        byte[] classBytes = readClassBytes(TargetGameService.class.getName());
        URL flaky = flakyCodeSourceUrl(classBytes);

        try (TestableKnotClassLoader loader = new TestableKnotClassLoader(new URL[0], PARENT)) {
            loader.resourceOverride = flaky;

            Class<?> loaded = loader.findClassDirect(TargetGameService.class.getName());

            assertThat(loaded.getClassLoader()).isSameAs(loader);
            assertThat(loaded.getProtectionDomain().getCodeSource().getLocation().toExternalForm())
                    .isEqualTo(flaky.toExternalForm());
        }
    }

    @Test
    @DisplayName("Should fall back to the external form for resources with unparseable URIs")
    void shouldUseExternalFormForUnparseableResourceUrl() throws Exception {
        Path resourcesDir = Files.createDirectories(tempDir.resolve("resources"));
        Files.createDirectories(resourcesDir.resolve("shared"));
        Files.writeString(resourcesDir.resolve("shared/data.txt"), "data");

        URL malformed = new URL("http", "example.com", "/invalid path.txt");

        try (TestableKnotClassLoader loader = new TestableKnotClassLoader(
                new URL[]{resourcesDir.toUri().toURL()}, PARENT)) {
            loader.collisionResources = Collections.enumeration(List.of(malformed));

            assertThat(loader.findResource("shared/data.txt")).isNotNull();
        }
    }

    @Test
    @DisplayName("Should tolerate failures while enumerating colliding resources")
    void shouldTolerateCollisionEnumerationFailure() throws Exception {
        Path resourcesDir = Files.createDirectories(tempDir.resolve("resources"));
        Files.createDirectories(resourcesDir.resolve("shared"));
        Files.writeString(resourcesDir.resolve("shared/data.txt"), "data");

        try (TestableKnotClassLoader loader = new TestableKnotClassLoader(
                new URL[]{resourcesDir.toUri().toURL()}, PARENT)) {
            loader.collisionFailure = new IOException("enumeration failure");

            assertThat(loader.findResource("shared/data.txt")).isNotNull();
        }
    }

    @Test
    @DisplayName("Should report the first occurrence and enumerate all colliding resources")
    void shouldFindResourceAcrossCollidingEntries() throws Exception {
        Path firstDir = Files.createDirectories(tempDir.resolve("first"));
        Path secondDir = Files.createDirectories(tempDir.resolve("second"));
        Files.createDirectories(firstDir.resolve("shared"));
        Files.createDirectories(secondDir.resolve("shared"));
        Files.writeString(firstDir.resolve("shared/data.txt"), "first");
        Files.writeString(secondDir.resolve("shared/data.txt"), "second");

        try (KnotClassLoader loader = new KnotClassLoader(
                new URL[]{firstDir.toUri().toURL(), secondDir.toUri().toURL()}, PARENT)) {
            URL resource = loader.findResource("shared/data.txt");

            assertThat(resource).isNotNull();
            assertThat(resource.toExternalForm()).contains("first");
            assertThat(Collections.list(loader.findResources("shared/data.txt"))).hasSize(2);
            assertThat(loader.findResource("shared/data.txt")).isNotNull();
        }
    }

    @Test
    @DisplayName("Should return null for missing and empty resource names")
    void shouldReturnNullForMissingAndEmptyResources() throws Exception {
        try (KnotClassLoader loader = new KnotClassLoader(new URL[0], PARENT)) {
            assertThat(loader.findResource("missing.txt")).isNull();
            assertThat(loader.findResource("")).isNull();
        }
    }

    @Test
    @DisplayName("Should deduplicate URLs that normalize to the same location")
    void shouldDeduplicateNormalizedUrls() throws Exception {
        Files.createDirectories(tempDir.resolve("norm"));
        String rawPath = tempDir.toUri().getRawPath();
        URL direct = new URL("file:" + rawPath + "norm");
        URL dotted = new URL("file:" + rawPath + "norm/../norm");

        try (KnotClassLoader loader = new KnotClassLoader(new URL[]{direct}, PARENT)) {
            loader.addURL(dotted);

            assertThat(loader.getURLs()).containsExactly(direct);
        }
    }

    @Test
    @DisplayName("Should format display names for non-file, root, and fragment URLs")
    void shouldFormatDisplayNamesForSpecialUrls() throws Exception {
        try (KnotClassLoader loader = new KnotClassLoader(new URL[0], PARENT)) {
            loader.addURL(new URL("http://example.com/plugin.jar"));
            loader.addURL(new URL("file", "", "/"));
            loader.addURL(new URL("file:" + tempDir.toUri().getRawPath() + "placeholder.txt#fragment"));

            assertThat(loader.getURLs()).hasSize(3);
        }
    }

    @Test
    @DisplayName("Should reject URLs that cannot be converted to a URI")
    void shouldRejectUnparseableUrl() throws Exception {
        try (KnotClassLoader loader = new KnotClassLoader(new URL[0], PARENT)) {
            URL malformed = new URL("http", "example.com", "/invalid path.txt");

            assertThatThrownBy(() -> loader.addURL(malformed))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid URI syntax");
        }
    }

    @Test
    @DisplayName("Should reuse an already defined package for additional local classes")
    void shouldReuseDefinedPackage() throws Exception {
        Path classesDir = Files.createDirectories(tempDir.resolve("classes"));
        writeClassFile(classesDir, TargetGameService.class.getName());

        byte[] patched = replaceAscii(
                readClassBytes(TargetGameService.class.getName()),
                "TargetGameService",
                "TargetGameServicX");
        Path patchedFile = classesDir.resolve("test/game/TargetGameServicX.class");
        Files.createDirectories(patchedFile.getParent());
        Files.write(patchedFile, patched);

        try (KnotClassLoader loader = new KnotClassLoader(
                new URL[]{classesDir.toUri().toURL()}, PARENT)) {
            Class<?> first = loader.loadClass(TargetGameService.class.getName());
            Class<?> second = loader.loadClass("test.game.TargetGameServicX");

            assertThat(first.getPackage()).isNotNull();
            assertThat(second.getPackage()).isSameAs(first.getPackage());
            assertThat(second.getPackage().getName()).isEqualTo("test.game");
        }
    }

    @Test
    @DisplayName("Should ignore classes that live in the default package")
    void shouldIgnoreDefaultPackageClassNames() throws Exception {
        try (KnotClassLoader loader = new KnotClassLoader(new URL[0], PARENT)) {
            Method definePackage = KnotClassLoader.class.getDeclaredMethod(
                    "definePackageIfNecessary", String.class);
            definePackage.setAccessible(true);

            assertThatCode(() -> definePackage.invoke(loader, "PlainClass"))
                    .doesNotThrowAnyException();
        }
    }

    @Test
    @DisplayName("Should ignore concurrent package definition conflicts")
    void shouldIgnoreConcurrentPackageDefinitionConflict() throws Exception {
        Path classesDir = Files.createDirectories(tempDir.resolve("classes"));
        writeClassFile(classesDir, TargetGameService.class.getName());

        try (TestableKnotClassLoader loader = new TestableKnotClassLoader(
                new URL[]{classesDir.toUri().toURL()}, PARENT)) {
            loader.rejectPackageDefinition = true;

            Class<?> loaded = loader.findClassDirect(TargetGameService.class.getName());

            assertThat(loaded.getClassLoader()).isSameAs(loader);
        }
    }

    @Test
    @DisplayName("Should define a class from transformer-provided bytes")
    void shouldUseTransformerProvidedBytes() throws Exception {
        Path classesDir = Files.createDirectories(tempDir.resolve("classes"));
        writeClassFile(classesDir, TargetGameService.class.getName());

        try (KnotClassLoader loader = new KnotClassLoader(
                new URL[]{classesDir.toUri().toURL()}, PARENT)) {
            loader.setClassTransformer((className, rawBytes) ->
                    replaceAscii(rawBytes, "Hello, ", "Howdy, "));

            Class<?> loaded = loader.loadClass(TargetGameService.class.getName());
            Object instance = loaded.getDeclaredConstructor().newInstance();
            Method greeting = loaded.getMethod("getGreeting", String.class);

            assertThat(greeting.invoke(instance, "World")).isEqualTo("Howdy, World");
        }
    }

    @Test
    @DisplayName("Should use the archive URL as the code source for classes loaded from a JAR")
    void shouldUseArchiveAsCodeSource() throws Exception {
        Path jar = tempDir.resolve("classes.jar");
        try (JarOutputStream jarOutput = new JarOutputStream(Files.newOutputStream(jar))) {
            jarOutput.putNextEntry(new JarEntry("test/game/TargetGameService.class"));
            jarOutput.write(readClassBytes(TargetGameService.class.getName()));
            jarOutput.closeEntry();
        }

        boolean previousUseCaches = URLConnection.getDefaultUseCaches("jar");
        URLConnection.setDefaultUseCaches("jar", false);
        try (KnotClassLoader loader = new KnotClassLoader(
                new URL[]{jar.toUri().toURL()}, PARENT)) {
            Class<?> loaded = loader.loadClass(TargetGameService.class.getName());
            URI location = loaded.getProtectionDomain().getCodeSource().getLocation().toURI();

            assertThat(loaded.getClassLoader()).isSameAs(loader);
            assertThat(Path.of(location)).isEqualTo(jar.toAbsolutePath().normalize());
        } finally {
            URLConnection.setDefaultUseCaches("jar", previousUseCaches);
        }
    }

    @Test
    @DisplayName("Should remain safe for resource lookups after close")
    void shouldHandleResourceLookupAfterClose() throws Exception {
        Path resourcesDir = Files.createDirectories(tempDir.resolve("resources"));
        Files.createDirectories(resourcesDir.resolve("shared"));
        Files.writeString(resourcesDir.resolve("shared/data.txt"), "data");

        KnotClassLoader loader = new KnotClassLoader(
                new URL[]{resourcesDir.toUri().toURL()}, PARENT);

        assertThat(loader.findResource("shared/data.txt")).isNotNull();

        loader.close();
        loader.close();

        assertThatCode(() -> loader.findResource("shared/data.txt"))
                .doesNotThrowAnyException();
        assertThat(loader.findResource("shared/absent.txt")).isNull();
    }

    private static void writeClassFile(Path root, String className) throws IOException {
        String resourcePath = className.replace('.', '/').concat(".class");
        Path target = root.resolve(resourcePath.replace('/', File.separatorChar));
        Files.createDirectories(target.getParent());
        Files.write(target, readClassBytes(className));
    }

    private static byte[] readClassBytes(String className) throws IOException {
        String resourcePath = className.replace('.', '/').concat(".class");
        try (InputStream inputStream = PARENT.getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new IOException("Class resource not found: " + resourcePath);
            }
            return inputStream.readAllBytes();
        }
    }

    private static byte[] replaceAscii(byte[] source, String from, String to) {
        byte[] needle = from.getBytes(StandardCharsets.US_ASCII);
        byte[] replacement = to.getBytes(StandardCharsets.US_ASCII);
        if (needle.length != replacement.length) {
            throw new IllegalArgumentException("Replacement must have the same length as the pattern");
        }

        byte[] result = source.clone();
        boolean replaced = false;
        for (int index = 0; index <= result.length - needle.length; index++) {
            boolean matches = true;
            for (int offset = 0; offset < needle.length; offset++) {
                if (result[index + offset] != needle[offset]) {
                    matches = false;
                    break;
                }
            }
            if (matches) {
                System.arraycopy(replacement, 0, result, index, replacement.length);
                replaced = true;
                index += needle.length - 1;
            }
        }

        if (!replaced) {
            throw new IllegalArgumentException("Pattern not found in byte array: " + from);
        }
        return result;
    }

    private static URL failingReadUrl() throws MalformedURLException {
        URLStreamHandler handler = new URLStreamHandler() {
            @Override
            protected URLConnection openConnection(URL url) {
                return new URLConnection(url) {
                    @Override
                    public void connect() {
                    }

                    @Override
                    public InputStream getInputStream() throws IOException {
                        throw new IOException("read failure");
                    }
                };
            }
        };
        return new URL(null, "avrix-test:broken", handler);
    }

    private static URL flakyCodeSourceUrl(byte[] classBytes) throws MalformedURLException {
        AtomicInteger opens = new AtomicInteger();
        URLStreamHandler handler = new URLStreamHandler() {
            @Override
            protected URLConnection openConnection(URL url) throws IOException {
                if (opens.incrementAndGet() == 1) {
                    return new URLConnection(url) {
                        @Override
                        public void connect() {
                        }

                        @Override
                        public InputStream getInputStream() {
                            return new ByteArrayInputStream(classBytes);
                        }
                    };
                }
                throw new IOException("code source connection failure");
            }
        };
        return new URL(null, "avrix-test:class", handler);
    }

    /**
     * Parent loader that fails only for the configured class name and delegates everything else.
     */
    private static final class SelectiveParent extends ClassLoader {

        private final String missingName;
        private final ClassLoader delegate;

        private SelectiveParent(String missingName, ClassLoader delegate) {
            super(delegate);
            this.missingName = missingName;
            this.delegate = delegate;
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            if (missingName.equals(name)) {
                throw new ClassNotFoundException(name);
            }
            return delegate.loadClass(name);
        }
    }

    /**
     * Test subclass exposing protected hooks and controllable resource lookup behavior.
     */
    private static final class TestableKnotClassLoader extends KnotClassLoader {

        private URL resourceOverride;
        private Enumeration<URL> collisionResources;
        private IOException collisionFailure;
        private boolean rejectPackageDefinition;

        private TestableKnotClassLoader(URL[] urls, ClassLoader parent) {
            super(urls, parent);
        }

        private TestableKnotClassLoader(URL[] urls, ClassLoader parent, ClassLoadingPolicy policy) {
            super(urls, parent, policy);
        }

        private String resolveLibrary(String libName) {
            return findLibrary(libName);
        }

        private Class<?> loadResolved(String name) throws ClassNotFoundException {
            return loadClass(name, true);
        }

        private Class<?> findClassDirect(String name) throws ClassNotFoundException {
            return findClass(name);
        }

        @Override
        public URL findResource(String name) {
            return resourceOverride != null ? resourceOverride : super.findResource(name);
        }

        @Override
        public Enumeration<URL> findResources(String name) throws IOException {
            if (collisionFailure != null) {
                throw collisionFailure;
            }
            return collisionResources != null ? collisionResources : super.findResources(name);
        }

        @Override
        protected Package definePackage(String name, String specTitle, String specVersion,
                                        String specVendor, String implTitle, String implVersion,
                                        String implVendor, URL sealBase) {
            if (rejectPackageDefinition) {
                throw new IllegalArgumentException("Simulated concurrent package definition");
            }
            return super.definePackage(name, specTitle, specVersion, specVendor,
                    implTitle, implVersion, implVendor, sealBase);
        }
    }
}
