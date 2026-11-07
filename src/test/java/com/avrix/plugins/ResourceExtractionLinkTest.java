package com.avrix.plugins;

import com.avrix.core.Metadata;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies real-path confinement for resource extraction, including Windows directory junctions.
 */
public class ResourceExtractionLinkTest {

    @TempDir
    private Path directory;

    /**
     * Verifies that an existing nested directory link cannot redirect writes outside the selected root.
     *
     * @throws Exception if fixtures or a directory link cannot be created
     */
    @Test
    public void shouldRejectEscapingDirectoryLinkBeforeCreatingChildren() throws Exception {
        Path target = Files.createDirectories(directory.resolve("target"));
        Path outside = Files.createDirectories(directory.resolve("outside"));
        Path link = target.resolve("link");
        createDirectoryLink(link, outside);
        try {
            assertThatThrownBy(() -> plugin().extractDirectory("assets", target, true))
                    .isInstanceOf(IOException.class).hasMessageContaining("escapes the target root");
            assertThat(outside.resolve("new-directory")).doesNotExist();
        } finally {
            Files.delete(link);
        }
    }

    /**
     * Verifies the explicit policy allowing links whose real destinations remain inside the selected root.
     *
     * @throws Exception if fixtures or a directory link cannot be created
     */
    @Test
    public void shouldAllowDirectoryLinkInsideTarget() throws Exception {
        Path target = Files.createDirectories(directory.resolve("target"));
        Path actual = Files.createDirectories(target.resolve("actual"));
        Path link = target.resolve("link");
        createDirectoryLink(link, actual);
        try {
            assertThat(plugin().extractDirectory("assets", target, false)).isEqualTo(1);
            assertThat(Files.readString(actual.resolve("new-directory").resolve("file.txt"))).isEqualTo("resource");
            Files.writeString(actual.resolve("new-directory").resolve("file.txt"), "existing");
            assertThat(plugin().extractDirectory("assets", target, false)).isZero();
            assertThat(Files.readString(actual.resolve("new-directory").resolve("file.txt"))).isEqualTo("existing");
        } finally {
            Files.delete(link);
        }
    }

    private PluginData plugin() throws IOException {
        Path archive = directory.resolve("plugin.jar");
        try (var output = new JarOutputStream(Files.newOutputStream(archive))) {
            output.putNextEntry(new JarEntry("assets/link/new-directory/file.txt"));
            output.write("resource".getBytes(java.nio.charset.StandardCharsets.UTF_8));
            output.closeEntry();
        }
        return new PluginData(archive, null, null, new Metadata.Builder()
                .id("resource-test").name("Resources").version("1.0.0").build());
    }

    private void createDirectoryLink(Path link, Path target) throws Exception {
        if (!System.getProperty("os.name", "").toLowerCase(Locale.ROOT).startsWith("win")) {
            Files.createSymbolicLink(link, target);
            return;
        }
        Path output = directory.resolve("junction-output.txt");
        Process process = new ProcessBuilder("cmd.exe", "/c", "mklink", "/J", link.toString(), target.toString())
                .redirectErrorStream(true).redirectOutput(output.toFile()).start();
        try {
            assertThat(process.waitFor(10, TimeUnit.SECONDS)).isTrue();
            assertThat(process.exitValue()).withFailMessage(new String(Files.readAllBytes(output),
                    java.nio.charset.Charset.defaultCharset())).isZero();
        } finally {
            process.destroyForcibly();
        }
    }
}
