package com.avrix.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import test.game.PlainGameService;
import test.game.TargetGameService;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Regression tests for the observable classpath state after {@link KnotClassLoader#close()}.
 */
@DisplayName("KnotClassLoader closed classpath lifecycle")
class KnotClassLoaderIndexedJarLifecycleTest {

    private static final ClassLoader PARENT = ClassLoader.getPlatformClassLoader();

    @TempDir
    private Path tempDir;

    @Test
    @DisplayName("Should invalidate indexed resources and refuse unloaded local classes after close")
    void shouldInvalidateResourcesAndRefuseUnloadedClassesAfterClose() throws Exception {
        Path jar = createClassJar("indexed.jar", TargetGameService.class, PlainGameService.class);
        KnotClassLoader loader = new KnotClassLoader(new URL[]{jar.toUri().toURL()}, PARENT);

        try {
            loader.loadClass(TargetGameService.class.getName());
            loader.close();

            assertThat(loader.findIndexedResource(classResourcePath(TargetGameService.class))).isNull();
            assertThat(loader.findResource(classResourcePath(TargetGameService.class))).isNull();
            assertThat(loader.findIndexedResource(classResourcePath(PlainGameService.class))).isNull();
            assertThat(loader.findResource(classResourcePath(PlainGameService.class))).isNull();

            assertThatThrownBy(() -> loader.loadClass(PlainGameService.class.getName()))
                    .isInstanceOf(ClassNotFoundException.class);

            // The failed load and the closed loader must not keep the archive open.
            Files.delete(jar);
        } finally {
            loader.close();
        }
    }

    @Test
    @DisplayName("Should ignore addURL after close")
    void shouldIgnoreAddUrlAfterClose() throws Exception {
        Path registered = createClassJar("registered.jar", TargetGameService.class);
        Path late = createClassJar("late.jar", PlainGameService.class);
        KnotClassLoader loader = new KnotClassLoader(new URL[]{registered.toUri().toURL()}, PARENT);

        try {
            loader.loadClass(TargetGameService.class.getName());
            loader.close();

            URL lateUrl = late.toUri().toURL();
            long versionBeforeAdd = loader.getClasspathVersion();
            loader.addURL(lateUrl, true);

            assertThat(loader.getURLs()).doesNotContain(lateUrl);
            assertThat(loader.getClasspathVersion()).isEqualTo(versionBeforeAdd);
            assertThat(loader.findIndexedResource(classResourcePath(PlainGameService.class))).isNull();
            assertThat(loader.findResource(classResourcePath(PlainGameService.class))).isNull();
        } finally {
            loader.close();
        }
    }

    private Path createClassJar(String name, Class<?>... types) throws IOException {
        Path jar = tempDir.resolve(name);
        try (JarOutputStream output = new JarOutputStream(Files.newOutputStream(jar))) {
            for (Class<?> type : types) {
                addClassEntry(output, type);
            }
        }
        return jar;
    }

    private static void addClassEntry(JarOutputStream jar, Class<?> type) throws IOException {
        String resourcePath = classResourcePath(type);
        try (InputStream stream = type.getClassLoader().getResourceAsStream(resourcePath)) {
            if (stream == null) {
                throw new IOException("Class resource not found: " + resourcePath);
            }
            jar.putNextEntry(new JarEntry(resourcePath));
            jar.write(stream.readAllBytes());
            jar.closeEntry();
        }
    }

    private static String classResourcePath(Class<?> type) {
        return type.getName().replace('.', '/') + ".class";
    }
}
