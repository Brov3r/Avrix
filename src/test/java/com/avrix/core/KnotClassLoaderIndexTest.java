package com.avrix.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression tests for the optional class resource index in {@link KnotClassLoader}.
 */
@DisplayName("KnotClassLoader class resource index")
class KnotClassLoaderIndexTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("Should return an indexed class resource")
    void shouldReturnIndexedClassResource() throws Exception {
        Path jar = createJar("indexed.jar", "indexed/Only.class", "indexed");

        try (KnotClassLoader loader = new KnotClassLoader(new URL[0], getClass().getClassLoader())) {
            loader.addURL(jar.toUri().toURL(), true);

            URL resource = loader.findResource("indexed/Only.class");

            assertThat(resource).isNotNull();
            assertThat(resource.toExternalForm()).contains("indexed.jar");
        }
    }

    @Test
    @DisplayName("Should prefer an earlier non-indexed URL over a later indexed duplicate")
    void shouldPreferEarlierNonIndexedResource() throws Exception {
        Path first = createJar("first.jar", "shared/Duplicate.class", "first");
        Path second = createJar("second.jar", "shared/Duplicate.class", "second");

        try (KnotClassLoader loader = new KnotClassLoader(new URL[0], getClass().getClassLoader())) {
            loader.addURL(first.toUri().toURL(), false);
            loader.addURL(second.toUri().toURL(), true);

            URL resource = loader.findResource("shared/Duplicate.class");

            assertThat(resource).isNotNull();
            assertThat(resource.toExternalForm()).contains("first.jar");
        }
    }

    @Test
    @DisplayName("Should prefer an earlier indexed URL over a later non-indexed duplicate")
    void shouldPreferEarlierIndexedResource() throws Exception {
        Path first = createJar("first.jar", "shared/Duplicate.class", "first");
        Path second = createJar("second.jar", "shared/Duplicate.class", "second");

        try (KnotClassLoader loader = new KnotClassLoader(new URL[0], getClass().getClassLoader())) {
            loader.addURL(first.toUri().toURL(), true);
            loader.addURL(second.toUri().toURL(), false);

            URL resource = loader.findResource("shared/Duplicate.class");

            assertThat(resource).isNotNull();
            assertThat(resource.toExternalForm()).contains("first.jar");
        }
    }

    @Test
    @DisplayName("Should load a real class from an indexed JAR")
    void shouldLoadClassFromIndexedJar() throws Exception {
        Path jar = tempDir.resolve("classes.jar");
        try (JarOutputStream jarStream = new JarOutputStream(Files.newOutputStream(jar))) {
            addClassEntry(jarStream, test.game.TargetGameService.class);
        }

        try (KnotClassLoader loader = new KnotClassLoader(new URL[0], getClass().getClassLoader())) {
            loader.addURL(jar.toUri().toURL(), true);

            Class<?> loaded = loader.loadClass(test.game.TargetGameService.class.getName());

            assertThat(loaded.getClassLoader()).isSameAs(loader);
            assertThat(loaded.getDeclaredConstructor().newInstance()).isNotNull();
        }
    }

    private Path createJar(String name, String entryName, String content) throws IOException {
        Path jar = tempDir.resolve(name);
        try (JarOutputStream stream = new JarOutputStream(Files.newOutputStream(jar))) {
            stream.putNextEntry(new JarEntry(entryName));
            stream.write(content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            stream.closeEntry();
        }
        return jar;
    }

    private void addClassEntry(JarOutputStream jar, Class<?> type) throws IOException {
        String path = type.getName().replace('.', '/') + ".class";
        try (InputStream stream = type.getClassLoader().getResourceAsStream(path)) {
            if (stream == null) {
                throw new IOException("Class resource not found: " + path);
            }
            jar.putNextEntry(new JarEntry(path));
            jar.write(stream.readAllBytes());
            jar.closeEntry();
        }
    }
}
