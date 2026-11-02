package com.avrix.core;

import com.avrix.utils.Constants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link Constants} public values and initialization.
 */
@DisplayName("Constants Unit Tests")
class ConstantsTest {

    @Test
    @DisplayName("Should expose CLI flag names")
    void shouldExposeCliFlagNames() {
        assertThat(Constants.NO_REDIRECT_FLAG_NAME).isEqualTo("--no-redirect-log");
        assertThat(Constants.STRICT_FLAG_NAME).isEqualTo("--strict");
    }

    @Test
    @DisplayName("Should expose plugin and metadata file names")
    void shouldExposePluginFileNames() {
        assertThat(Constants.METADATA_NAME).isEqualTo("metadata.yml");
        assertThat(Constants.PLUGIN_ICON_NAME).isEqualTo("icon.png");
        assertThat(Constants.PLUGIN_EXTENSION).isEqualTo(".jar");
        assertThat(Constants.PLUGINS_FOLDER_NAME).isEqualTo("plugins");
        assertThat(Constants.DISABLED_PLUGINS_FILE_NAME).isEqualTo("disabled.txt");
        assertThat(Constants.METADATA_SCHEMA).isEqualTo(1);
    }

    @Test
    @DisplayName("Should expose loader identity and provider selection constants")
    void shouldExposeLoaderIdentity() {
        assertThat(Constants.LOADER_ID).isEqualTo("avrix-loader");
        assertThat(Constants.DEFAULT_GAME_PROVIDER_ID).isEqualTo("project-zomboid");
        assertThat(Constants.GAME_PROVIDER_PROPERTY).isEqualTo("avrix.provider");
        assertThat(Constants.LOADER_AUTHOR).isEqualTo("Brov3r");
        assertThat(Constants.LOADER_LICENSE).isEqualTo("MIT");
        assertThat(Constants.LOADER_CONTACTS).isEqualTo("https://github.com/Brov3r/Avrix");
    }

    @Test
    @DisplayName("Should populate the version and name from build properties")
    void shouldPopulateVersionAndName() {
        assertThat(Constants.LOADER_VERSION).isNotBlank().doesNotContain("%");
        assertThat(Constants.LOADER_NAME).isNotBlank().doesNotContain("%");
    }

    @Test
    @DisplayName("Should prevent instantiation of the constants utility class")
    void shouldPreventInstantiation() throws NoSuchMethodException {
        Constructor<Constants> constructor = Constants.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        assertThatThrownBy(constructor::newInstance)
                .isInstanceOf(InvocationTargetException.class)
                .hasCauseInstanceOf(UnsupportedOperationException.class);
    }
}
