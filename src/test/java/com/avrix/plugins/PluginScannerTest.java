package com.avrix.plugins;

import com.avrix.api.PluginArchive;
import com.avrix.api.PluginSource;
import com.avrix.core.Environment;
import com.avrix.core.LoaderConfig;
import com.avrix.provider.ZomboidModPluginSource;
import com.avrix.utils.Constants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

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
                .containsExactly("plugin.jar", "secondary.JAR");
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

    @Test
    @DisplayName("Should merge plugins directory with provider-supplied plugin sources")
    void shouldMergeProviderSuppliedSources() throws Exception {
        Path pluginsDir = tempDir.resolve("plugins");
        Files.createDirectories(pluginsDir);
        Files.createFile(pluginsDir.resolve("local.jar"));

        Path sourceJar = tempDir.resolve("source.jar");
        Files.createFile(sourceJar);

        PluginSource source = () -> List.of(new PluginArchive(
                sourceJar.toFile(),
                "source-id",
                "Source",
                "custom",
                Map.of(),
                false
        ));

        LoaderConfig config = LoaderConfig.defaults().withPluginsDirectory(pluginsDir);
        List<File> result = new PluginScanner(config, List.of(source)).scan();

        assertThat(result)
                .extracting(File::getName)
                .containsExactlyInAnyOrder("local.jar", "source.jar");
    }

    @Test
    @DisplayName("Should merge plugins directory with active mod.info plugin archives")
    void shouldMergePluginsDirectoryAndActiveModPlugins() throws Exception {
        Path pluginsDir = tempDir.resolve("plugins");
        Files.createDirectories(pluginsDir);
        Files.createFile(pluginsDir.resolve("local.jar"));

        Path gameRoot = tempDir.resolve("steamapps/common/ProjectZomboid");
        Files.createDirectories(gameRoot);

        Path userHome = tempDir.resolve("home");
        Path modsDir = userHome.resolve("Zomboid/mods");
        Files.createDirectories(modsDir);
        Files.writeString(modsDir.resolve("default.txt"), """
                mods
                {
                    mod = test-mod,
                }
                """);

        Path versionDir = modsDir.resolve("TestMod/42");
        Files.createDirectories(versionDir.resolve("media/java"));
        Files.writeString(versionDir.resolve("mod.info"), """
                id=test-mod
                jarPlugins=media/java/mod.jar
                """);
        Files.createFile(versionDir.resolve("media/java/mod.jar"));

        LoaderConfig config = LoaderConfig.defaults().withPluginsDirectory(pluginsDir);
        List<File> result = new PluginScanner(
                config,
                List.of(new ZomboidModPluginSource(gameRoot, userHome, Environment.CLIENT, null))
        ).scan();

        assertThat(result)
                .extracting(File::getName)
                .containsExactlyInAnyOrder("local.jar", "mod.jar");
    }
}
