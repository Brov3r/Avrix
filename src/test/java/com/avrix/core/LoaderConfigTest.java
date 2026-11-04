package com.avrix.core;

import com.avrix.utils.Constants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link LoaderConfig} defaults, validation, and copy operations.
 */
@DisplayName("LoaderConfig Unit Tests")
class LoaderConfigTest {

    @Test
    @DisplayName("Should expose default loader settings")
    void shouldProvideDefaults() {
        LoaderConfig config = LoaderConfig.defaults();

        assertThat(config.pluginsDirectory()).isEqualTo(Path.of(Constants.PLUGINS_FOLDER_NAME));
        assertThat(config.metadataFileName()).isEqualTo(Constants.METADATA_NAME);
        assertThat(config.pluginExtension()).isEqualTo(Constants.PLUGIN_EXTENSION);
        assertThat(config.pluginIconName()).isEqualTo(Constants.PLUGIN_ICON_NAME);
        assertThat(config.providerId()).isEqualTo(Constants.DEFAULT_GAME_PROVIDER_ID);
        assertThat(config.environment()).isEqualTo(Environment.AUTO);
        assertThat(config.redirectSystemStreams()).isTrue();
        assertThat(config.strict()).isFalse();
    }

    @Test
    @DisplayName("Should replace only the plugins directory")
    void shouldReplacePluginsDirectory() {
        Path newDirectory = Path.of("custom-plugins");
        LoaderConfig original = LoaderConfig.defaults();

        LoaderConfig updated = original.withPluginsDirectory(newDirectory);

        assertThat(updated.pluginsDirectory()).isEqualTo(newDirectory);
        assertThat(updated.metadataFileName()).isEqualTo(original.metadataFileName());
        assertThat(updated.pluginExtension()).isEqualTo(original.pluginExtension());
        assertThat(updated.pluginIconName()).isEqualTo(original.pluginIconName());
        assertThat(updated.providerId()).isEqualTo(original.providerId());
        assertThat(updated.environment()).isEqualTo(original.environment());
        assertThat(updated.strict()).isEqualTo(original.strict());
        assertThat(updated.redirectSystemStreams()).isEqualTo(original.redirectSystemStreams());
    }

    @Test
    @DisplayName("Should replace only the target environment")
    void shouldReplaceEnvironment() {
        LoaderConfig original = LoaderConfig.defaults();

        LoaderConfig updated = original.withEnvironment(Environment.CLIENT);

        assertThat(updated.environment()).isEqualTo(Environment.CLIENT);
        assertThat(updated.pluginsDirectory()).isEqualTo(original.pluginsDirectory());
        assertThat(updated.metadataFileName()).isEqualTo(original.metadataFileName());
        assertThat(updated.pluginExtension()).isEqualTo(original.pluginExtension());
        assertThat(updated.pluginIconName()).isEqualTo(original.pluginIconName());
        assertThat(updated.providerId()).isEqualTo(original.providerId());
        assertThat(updated.redirectSystemStreams()).isEqualTo(original.redirectSystemStreams());
        assertThat(updated.strict()).isEqualTo(original.strict());
    }

    @Test
    @DisplayName("Should replace only the strict-loading flag")
    void shouldReplaceStrictFlag() {
        LoaderConfig original = LoaderConfig.defaults();

        LoaderConfig updated = original.withStrict(true);

        assertThat(updated.strict()).isTrue();
        assertThat(updated.pluginsDirectory()).isEqualTo(original.pluginsDirectory());
        assertThat(updated.metadataFileName()).isEqualTo(original.metadataFileName());
        assertThat(updated.pluginExtension()).isEqualTo(original.pluginExtension());
        assertThat(updated.pluginIconName()).isEqualTo(original.pluginIconName());
        assertThat(updated.providerId()).isEqualTo(original.providerId());
        assertThat(updated.environment()).isEqualTo(original.environment());
        assertThat(updated.redirectSystemStreams()).isEqualTo(original.redirectSystemStreams());
    }

    @Test
    @DisplayName("Should normalize plugin extension and preserve environment override")
    void shouldNormalizePluginExtension() {
        LoaderConfig config = new LoaderConfig(
                Path.of("plugins"),
                "metadata.yml",
                "JAR",
                "icon.png",
                Constants.DEFAULT_GAME_PROVIDER_ID,
                Environment.SERVER,
                false,
                true
        );

        assertThat(config.pluginExtension()).isEqualTo(".jar");
        assertThat(config.environment()).isEqualTo(Environment.SERVER);
        assertThat(config.redirectSystemStreams()).isFalse();
        assertThat(config.strict()).isTrue();
    }

    @Test
    @DisplayName("Should reject null and blank required values")
    void shouldRejectInvalidValues() {
        assertThatThrownBy(() -> new LoaderConfig(null, "metadata.yml", ".jar", "icon.png", Constants.DEFAULT_GAME_PROVIDER_ID, Environment.BOTH, true, false))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Plugins directory");

        assertThatThrownBy(() -> new LoaderConfig(Path.of("plugins"), " ", ".jar", "icon.png", Constants.DEFAULT_GAME_PROVIDER_ID, Environment.BOTH, true, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Metadata file name");

        assertThatThrownBy(() -> new LoaderConfig(Path.of("plugins"), "metadata.yml", "", "icon.png", Constants.DEFAULT_GAME_PROVIDER_ID, Environment.BOTH, true, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Plugin extension");

        assertThatThrownBy(() -> new LoaderConfig(Path.of("plugins"), "metadata.yml", ".jar", " ", Constants.DEFAULT_GAME_PROVIDER_ID, Environment.BOTH, true, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Plugin icon name");

        assertThatThrownBy(() -> new LoaderConfig(Path.of("plugins"), "metadata.yml", ".jar", "icon.png", " ", Environment.BOTH, true, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Game provider ID");

        assertThatThrownBy(() -> new LoaderConfig(Path.of("plugins"), "metadata.yml", ".jar", "icon.png", null, Environment.BOTH, true, false))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Game provider ID");
    }
}
