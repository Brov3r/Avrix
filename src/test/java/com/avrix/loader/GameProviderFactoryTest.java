package com.avrix.loader;

import com.avrix.core.KnotClassLoader;
import com.avrix.api.GameProvider;
import com.avrix.provider.zomboid.ZomboidConstants;
import com.avrix.provider.zomboid.ZomboidGameProvider;
import com.avrix.utils.Constants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URL;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link GameProviderFactory} ServiceLoader discovery.
 */
@DisplayName("GameProviderFactory Unit Tests")
class GameProviderFactoryTest {

    @Test
    @DisplayName("Should discover the default Project Zomboid provider")
    void shouldDiscoverDefaultProvider() {
        GameProvider provider = GameProviderFactory.load(
                ZomboidConstants.PROVIDER_ID,
                getClass().getClassLoader()
        );

        assertThat(provider).isInstanceOf(ZomboidGameProvider.class);
        assertThat(provider.getId()).isEqualTo(ZomboidConstants.PROVIDER_ID);
    }

    @Test
    @DisplayName("Should discover the provider through KnotClassLoader")
    void shouldDiscoverProviderThroughKnotClassLoader() throws IOException {
        try (KnotClassLoader classLoader = new KnotClassLoader(new URL[0], getClass().getClassLoader())) {
            GameProvider provider = GameProviderFactory.load(ZomboidConstants.PROVIDER_ID, classLoader);

            assertThat(provider).isInstanceOf(ZomboidGameProvider.class);
            assertThat(provider.getId()).isEqualTo(ZomboidConstants.PROVIDER_ID);
        }
    }

    @Test
    @DisplayName("Should report available providers when the requested ID is missing")
    void shouldFailOnMissingProvider() {
        assertThatThrownBy(() -> GameProviderFactory.load("missing-provider", getClass().getClassLoader()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No GameProvider registered for ID [missing-provider]")
                .hasMessageContaining(ZomboidConstants.PROVIDER_ID);
    }

    @Test
    @DisplayName("Should reject null and blank provider IDs")
    void shouldRejectInvalidProviderId() {
        assertThatThrownBy(() -> GameProviderFactory.load(null, getClass().getClassLoader()))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Game provider ID");

        assertThatThrownBy(() -> GameProviderFactory.load(" ", getClass().getClassLoader()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Game provider ID");
    }

    @Test
    @DisplayName("Should reject a null class loader")
    void shouldRejectNullClassLoader() {
        assertThatThrownBy(() -> GameProviderFactory.load(ZomboidConstants.PROVIDER_ID, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("ClassLoader");
    }

    @Test
    @DisplayName("Should prevent direct factory instantiation")
    void shouldPreventDirectInstantiation() throws Exception {
        var constructor = GameProviderFactory.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        assertThatThrownBy(constructor::newInstance)
                .hasCauseInstanceOf(UnsupportedOperationException.class);
    }
}
