package com.avrix.loader;

import com.avrix.core.KnotClassLoader;
import com.avrix.core.Metadata;
import com.avrix.plugins.PluginData;
import com.avrix.plugins.PluginManager;
import org.junit.jupiter.api.Test;
import test.loader.TestGameProvider;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies launch validation before plugin initialization.
 */
public class LaunchValidationTest {

    /**
     * Verifies that invalid launch prerequisites cannot execute plugin manager operations.
     *
     * @throws Exception if the classloader cannot be closed
     */
    @Test
    public void shouldValidateBeforeInitializingPlugins() throws Exception {
        AtomicBoolean touched = new AtomicBoolean();
        var provider = new TestGameProvider() {
            /** {@inheritDoc} */
            @Override
            public void validateLaunch() {
                throw new IllegalStateException("Invalid working directory");
            }
        };
        PluginManager plugins = new PluginManager() {
            /** {@inheritDoc} */
            @Override
            public void init() {
                touched.set(true);
            }

            /** {@inheritDoc} */
            @Override
            public void loadPlugins() {
                touched.set(true);
            }

            /** {@inheritDoc} */
            @Override
            public void loadPlugin(PluginData container) {
                touched.set(true);
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
        try (var loader = new KnotClassLoader(new URL[0], getClass().getClassLoader());
             var context = new LoaderContext(loader, provider, null, plugins)) {
            assertThatThrownBy(context::initialize).hasMessage("Invalid working directory");
            assertThat(touched).isFalse();
        }
    }
}
