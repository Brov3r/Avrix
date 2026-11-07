package com.avrix.core;

import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that the class index preserves manifest classpath ordering.
 */
public class KnotClassLoaderManifestClasspathTest {

    @TempDir
    private Path directory;

    /**
     * Verifies all registration overloads, including pre-collected empty entry lists.
     *
     * @param mode registration overload to exercise
     * @throws IOException if fixture archives cannot be written or closed
     */
    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3})
    public void shouldPreferManifestDependency(int mode) throws IOException {
        Path first = createJar("first.jar", "dependency.jar", null);
        createJar("dependency.jar", null, "example/Duplicate.class");
        Path last = createJar("last.jar", null, "example/Duplicate.class");
        URL[] urls = {first.toUri().toURL(), last.toUri().toURL()};

        try (var standard = new URLClassLoader(urls, ClassLoader.getPlatformClassLoader());
             var loader = new KnotClassLoader(new URL[0], getClass().getClassLoader())) {
            switch (mode) {
                case 0 -> loader.addURL(urls[0]);
                case 1 -> loader.addURL(urls[0], true, List.of());
                case 2 -> loader.addURL(urls[0], true, List.of(), false);
                case 3 -> loader.addURL(urls[0], false);
                default -> throw new IllegalArgumentException("Unknown mode: " + mode);
            }
            loader.addURL(urls[1]);

            assertThat(loader.findIndexedResource("example/Duplicate.class")).isNull();
            assertThat(loader.findResource("example/Duplicate.class"))
                    .isEqualTo(standard.findResource("example/Duplicate.class"));
            assertThat(loader.findResource("example/Duplicate.class").toExternalForm()).contains("dependency.jar");
        }
    }

    private Path createJar(String name, String classPath, String entryName) throws IOException {
        Path path = directory.resolve(name);
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().putValue("Manifest-Version", "1.0");
        if (classPath != null) {
            manifest.getMainAttributes().putValue("Class-Path", classPath);
        }
        try (var output = new JarOutputStream(Files.newOutputStream(path), manifest)) {
            if (entryName != null) {
                output.putNextEntry(new JarEntry(entryName));
                output.write(1);
                output.closeEntry();
            }
        }
        return path;
    }
}
