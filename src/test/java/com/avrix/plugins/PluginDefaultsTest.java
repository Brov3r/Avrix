package com.avrix.plugins;

import com.avrix.core.Metadata;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for default lifecycle methods on {@link Plugin} and {@link PluginManager}.
 */
@DisplayName("Plugin Default Methods Unit Tests")
class PluginDefaultsTest {

    @Test
    @DisplayName("Should make plugin shutdown a no-op by default")
    void shouldMakePluginShutdownNoOp() {
        Plugin plugin = new Plugin() {
            /** {@inheritDoc} */
            @Override
            public void onInitialize(PluginData pluginData) {
            }
        };

        assertThatCode(plugin::onShutdown).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should return empty load results by default")
    void shouldReturnEmptyLoadResults() {
        PluginManager manager = minimalPluginManager();

        assertThat(manager.getLoadResults()).isEmpty();
        assertThatThrownBy(() -> manager.getLoadResults().add(PluginLoadResult.loaded("example-plugin")))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("Should make plugin manager close a no-op by default")
    void shouldMakePluginManagerCloseNoOp() {
        PluginManager manager = minimalPluginManager();

        assertThatCode(manager::close).doesNotThrowAnyException();
    }

    private static PluginManager minimalPluginManager() {
        return new PluginManager() {
            /** {@inheritDoc} */
            @Override
            public void init() {
            }

            /** {@inheritDoc} */
            @Override
            public void loadPlugins() {
            }

            /** {@inheritDoc} */
            @Override
            public void loadPlugin(PluginData container) {
            }

            /** {@inheritDoc} */
            @Override
            public Map<String, PluginData> getPlugins() {
                return Map.of();
            }

            /** {@inheritDoc} */
            @Override
            public List<Metadata> resolvePluginLoadOrder(List<Metadata> candidates) {
                return candidates;
            }
        };
    }
}
