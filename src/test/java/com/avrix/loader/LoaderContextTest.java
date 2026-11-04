package com.avrix.loader;

import com.avrix.core.Environment;
import com.avrix.core.KnotClassLoader;
import com.avrix.core.Metadata;
import com.avrix.plugins.PluginData;
import com.avrix.plugins.PluginManager;
import com.avrix.provider.GameProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link LoaderContext} lifecycle delegation.
 */
@DisplayName("LoaderContext Unit Tests")
class LoaderContextTest {

    @Test
    @DisplayName("Should initialize plugins and delegate game lifecycle operations")
    void shouldDelegateLifecycleOperations() throws Exception {
        try (KnotClassLoader classLoader = newClassLoader()) {
            RecordingGameProvider gameProvider = new RecordingGameProvider();
            RecordingPluginManager pluginManager = new RecordingPluginManager();
            LoaderContext context = new LoaderContext(classLoader, gameProvider, null, pluginManager);

            context.initialize();
            context.redirectSystemStreams();
            context.launch(new String[]{"-test"});

            assertThat(pluginManager.initialized).isTrue();
            assertThat(pluginManager.loaded).isTrue();
            assertThat(gameProvider.streamsRedirected).isTrue();
            assertThat(gameProvider.launchArgs).containsExactly("-test");
            assertThat(context.getGameName()).isEqualTo("Test Game");
        }
    }

    @Test
    @DisplayName("Should close plugins and classloader idempotently")
    void shouldCloseResourcesIdempotently() throws Exception {
        try (KnotClassLoader classLoader = newClassLoader()) {
            RecordingGameProvider gameProvider = new RecordingGameProvider();
            RecordingPluginManager pluginManager = new RecordingPluginManager();
            LoaderContext context = new LoaderContext(classLoader, gameProvider, null, pluginManager);

            context.close();
            context.close();

            assertThat(pluginManager.closed).isTrue();
        }
    }

    @Test
    @DisplayName("Should reject null dependencies")
    void shouldRejectNullDependencies() throws Exception {
        try (KnotClassLoader classLoader = newClassLoader()) {
            RecordingGameProvider gameProvider = new RecordingGameProvider();
            RecordingPluginManager pluginManager = new RecordingPluginManager();

            assertThatThrownBy(() -> new LoaderContext(null, gameProvider, null, pluginManager))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("KnotClassLoader");

            assertThatThrownBy(() -> new LoaderContext(classLoader, null, null, pluginManager))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("GameProvider");

            assertThatThrownBy(() -> new LoaderContext(classLoader, gameProvider, null, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("PluginManager");
        }
    }

    private static KnotClassLoader newClassLoader() {
        return new KnotClassLoader(new URL[0], LoaderContextTest.class.getClassLoader());
    }

    /**
     * Recording game provider used to verify lifecycle delegation.
     */
    private static final class RecordingGameProvider implements GameProvider {
        private boolean streamsRedirected;
        private String[] launchArgs = new String[0];

        /** {@inheritDoc} */
        @Override
        public void init() {
        }

        /** {@inheritDoc} */
        @Override
        public void launch(String[] args) {
            launchArgs = args.clone();
        }

        /** {@inheritDoc} */
        @Override
        public String[] getLaunchArgs() {
            return launchArgs.clone();
        }

        /** {@inheritDoc} */
        @Override
        public String getName() {
            return "Test Game";
        }

        /** {@inheritDoc} */
        @Override
        public String getId() {
            return "test-game";
        }

        /** {@inheritDoc} */
        @Override
        public List<String> getAuthors() {
            return List.of("Test Author");
        }

        /** {@inheritDoc} */
        @Override
        public String getLicense() {
            return "MIT";
        }

        /** {@inheritDoc} */
        @Override
        public List<String> getContacts() {
            return List.of();
        }

        /** {@inheritDoc} */
        @Override
        public String getNormalizedVersion() {
            return "1.0.0";
        }

        /** {@inheritDoc} */
        @Override
        public String getRawVersion() {
            return "1.0.0";
        }

        /** {@inheritDoc} */
        @Override
        public Path getLaunchDirectory() {
            return Path.of(".");
        }

        /** {@inheritDoc} */
        @Override
        public String getEntrypoint() {
            return "test.Main";
        }

        /** {@inheritDoc} */
        @Override
        public Environment getEnvironment() {
            return Environment.BOTH;
        }

        /** {@inheritDoc} */
        @Override
        public void redirectSystemStreamsToLogger() {
            streamsRedirected = true;
        }
    }

    /**
     * Recording plugin manager used to verify lifecycle delegation.
     */
    private static final class RecordingPluginManager implements PluginManager {
        private boolean initialized;
        private boolean loaded;
        private boolean closed;

        /** {@inheritDoc} */
        @Override
        public void init() {
            initialized = true;
        }

        /** {@inheritDoc} */
        @Override
        public void loadPlugins() {
            loaded = true;
        }

        /** {@inheritDoc} */
        @Override
        public void close() {
            closed = true;
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
    }
}
