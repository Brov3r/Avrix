package com.avrix.plugins;

import com.avrix.core.Environment;
import com.avrix.core.Metadata;
import com.avrix.utils.Constants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests lazy per-plugin data directory resolution in {@link PluginData}.
 */
@DisplayName("PluginData data directory tests")
class PluginDataDataDirectoryTest {

    @TempDir
    Path tempDir;

    private Metadata metadata;
    private Path jarPath;

    @BeforeEach
    void setUp() throws IOException {
        metadata = metadata("test-plugin", "1.0.0");
        jarPath = createJar(tempDir.resolve("test-plugin.jar"), "test/Plugin.class");
    }

    @Test
    @DisplayName("Should create the directory only when it is first requested")
    void shouldCreateDirectoryLazily() throws IOException {
        Path dataDirectory = tempDir.resolve("plugins/test-plugin");
        PluginData pluginData = new PluginData(jarPath, null, null, null, metadata, dataDirectory);

        assertThat(dataDirectory).doesNotExist();
        assertThat(pluginData.getDataDirectory()).contains(dataDirectory.toAbsolutePath().normalize());
        assertThat(dataDirectory).isDirectory();

        Files.writeString(dataDirectory.resolve("state.txt"), "persisted");
        assertThat(dataDirectory.resolve("state.txt")).hasContent("persisted");
    }

    @Test
    @DisplayName("Should share one data directory between all handlers of a plugin")
    void shouldShareDirectoryBetweenHandlers() {
        Path dataDirectory = tempDir.resolve("plugins/shared");
        Plugin first = pluginData -> {
        };
        Plugin second = pluginData -> {
        };

        PluginData pluginData = new PluginData(jarPath, null, first, List.of(first, second), metadata, dataDirectory);

        assertThat(pluginData.instances()).hasSize(2);
        assertThat(pluginData.getDataDirectory()).contains(dataDirectory.toAbsolutePath().normalize());
        assertThat(pluginData.instance()).isSameAs(first);
    }

    @Test
    @DisplayName("Should expose distinct directories for distinct plugins")
    void shouldExposeDistinctDirectories() {
        PluginData first = new PluginData(
                jarPath,
                null,
                null,
                null,
                metadata,
                tempDir.resolve("plugins/first")
        );
        PluginData second = new PluginData(
                jarPath,
                null,
                null,
                null,
                metadata("second-plugin", "1.0.0"),
                tempDir.resolve("plugins/second")
        );

        assertThat(first.getDataDirectory()).isPresent();
        assertThat(second.getDataDirectory()).isPresent();
        assertThat(first.getDataDirectory()).isNotEqualTo(second.getDataDirectory());
    }

    @Test
    @DisplayName("Should return an empty directory for synthetic and unconfigured descriptors")
    void shouldReturnEmptyWhenNotApplicable() {
        PluginData synthetic = new PluginData(metadata);
        PluginData withoutDirectory = new PluginData(jarPath, null, null, metadata);

        assertThat(synthetic.getDataDirectory()).isEmpty();
        assertThat(withoutDirectory.getDataDirectory()).isEmpty();
        assertThat(withoutDirectory.getPluginPath()).isPresent();
    }

    @Test
    @DisplayName("Should keep the directory inside the plugins root")
    void shouldKeepDirectoryInsidePluginsRoot() {
        Path pluginsRoot = tempDir.resolve("plugins").toAbsolutePath().normalize();
        Path dataDirectory = pluginsRoot.resolve("nested-plugin");
        PluginData pluginData = new PluginData(jarPath, null, null, null, metadata, dataDirectory);

        Path resolved = pluginData.getDataDirectory().orElseThrow();

        assertThat(resolved).isAbsolute().isNormalized();
        assertThat(resolved.startsWith(pluginsRoot)).isTrue();
        assertThat(resolved.getParent()).isEqualTo(pluginsRoot);
    }

    @Test
    @DisplayName("Should fail with UncheckedIOException when a file occupies the directory path")
    void shouldFailWhenFileOccupiesDirectoryPath() throws IOException {
        Path occupied = tempDir.resolve("plugins/occupied");
        Files.createDirectories(occupied.getParent());
        Files.writeString(occupied, "not a directory");

        PluginData pluginData = new PluginData(jarPath, null, null, null, metadata, occupied);

        assertThatThrownBy(pluginData::getDataDirectory)
                .isInstanceOf(UncheckedIOException.class)
                .hasMessageContaining("occupied");
    }

    private static Metadata metadata(String id, String version) {
        return new Metadata.Builder()
                .schema(Constants.METADATA_SCHEMA)
                .id(id)
                .name(id)
                .version(version)
                .environment(Environment.BOTH)
                .build();
    }

    private static Path createJar(Path destination, String... classEntries) throws IOException {
        Files.createDirectories(destination.getParent());
        try (JarOutputStream jar = new JarOutputStream(Files.newOutputStream(destination))) {
            for (String classEntry : classEntries) {
                jar.putNextEntry(new JarEntry(classEntry));
                jar.closeEntry();
            }
        }
        return destination;
    }
}
