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
import java.net.URLClassLoader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

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
                    .isInstanceOf(ClassNotFoundException.class)
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

            classes.put("other.Placeholder", () -> new byte[0]);

            assertThat(classes).containsKey("other.Placeholder");
        }
    }

    @Test
    @DisplayName("Should invalidate cached bytecode after a new classpath URL is registered")
    void shouldInvalidateCacheAfterUrlRegistration() throws Exception {
        Path classesDir = Files.createDirectories(tempDir.resolve("late-classes"));
        String classPath = TargetGameService.class.getName().replace('.', '/') + ".class";
        writeBytes(classesDir.resolve(classPath), readClassBytes(TargetGameService.class));

        try (KnotClassLoader loader = new KnotClassLoader(new URL[0], PARENT)) {
            CompositeClassProvider provider = new CompositeClassProvider(loader);
            byte[] beforeRegistration = provider.getClass(TargetGameService.class.getName());

            loader.addURL(classesDir.toUri().toURL());
            byte[] afterRegistration = provider.getClass(TargetGameService.class.getName());

            assertThat(afterRegistration).isNotSameAs(beforeRegistration);
        }
    }

    @Test
    @DisplayName("Should resolve versioned classes from multi-release JARs during class scanning")
    void shouldResolveMultiReleaseJarClassesDuringClassScan() throws Exception {
        Path jarPath = tempDir.resolve("multi-release.jar");
        String classPath = TargetGameService.class.getName().replace('.', '/') + ".class";
        byte[] baseBytes = readClassBytes(TargetGameService.class);
        byte[] versionedBytes = {0x01, 0x02, 0x03};
        String versionedPath = "META-INF/versions/" + Runtime.version().feature() + "/" + classPath;

        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        manifest.getMainAttributes().put(new Attributes.Name("Multi-Release"), "true");

        try (JarOutputStream outputStream = new JarOutputStream(Files.newOutputStream(jarPath))) {
            outputStream.putNextEntry(new JarEntry("META-INF/MANIFEST.MF"));
            manifest.write(outputStream);
            outputStream.closeEntry();

            outputStream.putNextEntry(new JarEntry(classPath));
            outputStream.write(baseBytes);
            outputStream.closeEntry();

            outputStream.putNextEntry(new JarEntry(versionedPath));
            outputStream.write(versionedBytes);
            outputStream.closeEntry();

            outputStream.putNextEntry(new JarEntry("META-INF/versions/" + Runtime.version().feature()
                    + "/versioned/Only.class"));
            outputStream.write(versionedBytes);
            outputStream.closeEntry();
        }

        try (KnotClassLoader loader = new KnotClassLoader(new URL[]{jarPath.toUri().toURL()}, PARENT)) {
            Map<String, Supplier<byte[]>> classes = new CompositeClassProvider(loader).getAllClasses();

            assertThat(classes).containsKey(TargetGameService.class.getName());
            assertThat(classes.get(TargetGameService.class.getName()).get()).isEqualTo(versionedBytes);
            assertThat(classes).containsKey("versioned.Only");
            assertThat(classes.get("versioned.Only").get()).isEqualTo(versionedBytes);
        }
    }

    /**
     * Verifies local fallback for delegated packages whose classes are absent from the parent.
     *
     * @throws Exception if fixture creation or class loading fails
     */
    @Test
    public void shouldResolveLocalClassInParentDelegatedPackage() throws Exception {
        String name = "com.avrix.fixture.LocalOnly";
        byte[] bytes = fixtureClass(name);
        Path jar = tempDir.resolve("local.jar");
        createJar(jar, Map.of(name.replace('.', '/') + ".class", bytes));
        try (var loader = new KnotClassLoader(new URL[]{jar.toUri().toURL()}, PARENT)) {
            var provider = new CompositeClassProvider(loader);
            assertThat(loader.loadClass(name).getClassLoader()).isSameAs(loader);
            assertThat(provider.getClass(name)).isEqualTo(bytes);
            assertThat(provider.getAllClasses().get(name).get()).isEqualTo(bytes);
        }
    }

    /**
     * Verifies that the actual parent supplies delegated bytecode ahead of colliding local classes.
     *
     * @throws Exception if fixture creation or class loading fails
     */
    @Test
    public void shouldUseActualParentBeforeLocalForDelegatedClasses() throws Exception {
        String name = "com.avrix.fixture.ParentClass";
        String resource = name.replace('.', '/') + ".class";
        byte[] parentBytes = fixtureClass(name);
        Path parentJar = tempDir.resolve("parent.jar");
        Path localJar = tempDir.resolve("collision.jar");
        createJar(parentJar, Map.of(resource, parentBytes));
        createJar(localJar, Map.of(resource, new byte[]{1, 2, 3}));

        try (var parent = new URLClassLoader(new URL[]{parentJar.toUri().toURL()}, PARENT);
             var loader = new KnotClassLoader(new URL[]{localJar.toUri().toURL()}, parent)) {
            var provider = new CompositeClassProvider(loader);
            assertThat(loader.loadClass(name).getClassLoader()).isSameAs(parent);
            assertThat(provider.getClass(name)).isEqualTo(parentBytes);
            assertThat(provider.getAllClasses().get(name).get()).isEqualTo(parentBytes);
        }
    }

    /**
     * Verifies local-first resolution and actual-parent fallback for ordinary plugin packages.
     *
     * @throws Exception if fixture creation or class loading fails
     */
    @Test
    public void shouldUseLocalThenActualParentForPluginClasses() throws Exception {
        String localName = "example.fixture.Local";
        String parentName = "example.fixture.Parent";
        byte[] localBytes = fixtureClass(localName);
        byte[] parentBytes = fixtureClass(parentName);
        Path parentJar = tempDir.resolve("parent.jar");
        Path localJar = tempDir.resolve("local.jar");
        createJar(parentJar, Map.of(parentName.replace('.', '/') + ".class", parentBytes,
                localName.replace('.', '/') + ".class", new byte[]{1, 2, 3}));
        createJar(localJar, Map.of(localName.replace('.', '/') + ".class", localBytes));

        try (var parent = new URLClassLoader(new URL[]{parentJar.toUri().toURL()}, PARENT);
             var loader = new KnotClassLoader(new URL[]{localJar.toUri().toURL()}, parent)) {
            var provider = new CompositeClassProvider(loader);
            assertThat(loader.loadClass(localName).getClassLoader()).isSameAs(loader);
            assertThat(loader.loadClass(parentName).getClassLoader()).isSameAs(parent);
            assertThat(provider.getClass(localName)).isEqualTo(localBytes);
            assertThat(provider.getClass(parentName)).isEqualTo(parentBytes);
        }
    }

    private static byte[] fixtureClass(String name) {
        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V25, Opcodes.ACC_PUBLIC, name.replace('.', '/'), null, "java/lang/Object", null);
        writer.visitEnd();
        return writer.toByteArray();
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
