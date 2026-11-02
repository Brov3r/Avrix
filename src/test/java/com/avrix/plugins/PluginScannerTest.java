package com.avrix.plugins;

import com.avrix.core.LoaderConfig;
import com.avrix.utils.Constants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link PluginScanner}.
 */
@DisplayName("PluginScanner Unit Tests")
class PluginScannerTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("Should find plugin archives and ignore unrelated files and directories")
    void shouldFindPluginArchives() throws Exception {
        Files.createFile(tempDir.resolve("plugin.jar"));
        Files.createFile(tempDir.resolve("secondary.JAR"));
        Files.createFile(tempDir.resolve("readme.txt"));
        Files.createDirectories(tempDir.resolve("directory.jar"));

        LoaderConfig config = LoaderConfig.defaults().withPluginsDirectory(tempDir);
        List<File> pluginFiles = new PluginScanner(config).scan();

        assertThat(pluginFiles)
                .extracting(File::getName)
                .containsExactlyInAnyOrder("plugin.jar", "secondary.JAR");
    }

    @Test
    @DisplayName("Should return an empty list when the plugins directory is missing")
    void shouldReturnEmptyWhenDirectoryIsMissing() {
        LoaderConfig config = LoaderConfig.defaults().withPluginsDirectory(tempDir.resolve("missing"));
        List<File> pluginFiles = new PluginScanner(config).scan();

        assertThat(pluginFiles).isEmpty();
        assertThat(new PluginScanner(config).readDisabledPlugins()).isEmpty();
    }

    @Test
    @DisplayName("Should read disabled plugin entries and ignore comments")
    void shouldReadDisabledPlugins() throws Exception {
        Files.writeString(tempDir.resolve(Constants.DISABLED_PLUGINS_FILE_NAME), """
                # Disabled plugins
                Plugin-ID
                other.jar # disabled by archive name
                """);

        LoaderConfig config = LoaderConfig.defaults().withPluginsDirectory(tempDir);
        var disabled = new PluginScanner(config).readDisabledPlugins();

        assertThat(disabled).containsExactlyInAnyOrder("other.jar", "plugin-id");
    }

    @Test
    @DisplayName("Should return empty results when the plugins location is a regular file")
    void shouldReturnEmptyWhenPluginsLocationIsAFile() throws Exception {
        Path file = tempDir.resolve("not-a-directory");
        Files.writeString(file, "content");

        LoaderConfig config = LoaderConfig.defaults().withPluginsDirectory(file);
        PluginScanner scanner = new PluginScanner(config);

        assertThat(scanner.scan()).isEmpty();
        assertThat(scanner.readDisabledPlugins()).isEmpty();
    }

    @Test
    @DisplayName("Should ignore blank lines and trim padded disabled entries")
    void shouldIgnoreBlankLinesInDisabledList() throws Exception {
        Files.writeString(
                tempDir.resolve(Constants.DISABLED_PLUGINS_FILE_NAME),
                "\n   \n  Padded-ID  \n\t\n"
        );

        LoaderConfig config = LoaderConfig.defaults().withPluginsDirectory(tempDir);
        var disabled = new PluginScanner(config).readDisabledPlugins();

        assertThat(disabled).containsExactly("padded-id");
    }
}
