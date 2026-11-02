package com.avrix.loader;

import com.avrix.core.Environment;
import com.avrix.core.KnotClassLoader;
import com.avrix.core.Metadata;
import com.avrix.plugins.PluginData;
import com.avrix.plugins.PluginManager;
import com.avrix.provider.GameProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Edge-case tests for {@link LoaderContext} shutdown failure handling and idempotency.
 */
@DisplayName("LoaderContext Edge Case Tests")
class LoaderContextEdgeTest {

    @Test
    @DisplayName("Should close the classloader even when the plugin manager fails")
    void shouldCloseClassLoaderWhenPluginManagerFails() {
        RecordingPluginManager pluginManager = new RecordingPluginManager();
        pluginManager.failure = new IllegalStateException("plugin shutdown failure");
        CountingClassLoader classLoader = new CountingClassLoader();
        LoaderContext context = new LoaderContext(classLoader, new StubGameProvider(), pluginManager);

        assertThatCode(context::close).doesNotThrowAnyException();

        assertThat(pluginManager.closeCalls).isEqualTo(1);
        assertThat(classLoader.closeCalls).isEqualTo(1);
    }

    @Test
    @DisplayName("Should tolerate an IOException from the classloader close")
    void shouldTolerateClassLoaderCloseFailure() {
        CountingClassLoader classLoader = new CountingClassLoader();
        classLoader.failure = new IOException("classloader shutdown failure");
        LoaderContext context = new LoaderContext(classLoader, new StubGameProvider(), new RecordingPluginManager());

        assertThatCode(context::close).doesNotThrowAnyException();

        assertThat(classLoader.closeCalls).isEqualTo(1);
    }

    @Test
    @DisplayName("Should close each dependency only once")
    void shouldCloseEachDependencyOnce() {
        CountingClassLoader classLoader = new CountingClassLoader();
        RecordingPluginManager pluginManager = new RecordingPluginManager();
        LoaderContext context = new LoaderContext(classLoader, new StubGameProvider(), pluginManager);

        context.close();
        context.close();

        assertThat(pluginManager.closeCalls).isEqualTo(1);
        assertThat(classLoader.closeCalls).isEqualTo(1);
    }

    @Test
    @DisplayName("Should tolerate a classloader that is already closed")
    void shouldTolerateAlreadyClosedClassLoader() throws IOException {
        CountingClassLoader classLoader = new CountingClassLoader();
        classLoader.close();
        LoaderContext context = new LoaderContext(classLoader, new StubGameProvider(), new RecordingPluginManager());

        assertThatCode(context::close).doesNotThrowAnyException();

        assertThat(classLoader.closeCalls).isEqualTo(2);
    }

    /**
     * Classloader that records close invocations and can simulate a shutdown failure.
     */
    private static final class CountingClassLoader extends KnotClassLoader {

        private int closeCalls;
        private IOException failure;

        private CountingClassLoader() {
            super(new URL[0], LoaderContextEdgeTest.class.getClassLoader());
        }

        @Override
        public void close() throws IOException {
            closeCalls++;
            if (failure != null) {
                throw failure;
            }
            super.close();
        }
    }

    /**
     * Plugin manager that records close invocations and can simulate a shutdown failure.
     */
    private static final class RecordingPluginManager implements PluginManager {

        private int closeCalls;
        private Throwable failure;

        @Override
        public void init() {
        }

        @Override
        public void loadPlugins() {
        }

        @Override
        public void close() {
            closeCalls++;
            if (failure != null) {
                throw new IllegalStateException(failure);
            }
        }

        @Override
        public void loadPlugin(PluginData container) {
        }

        @Override
        public Map<String, PluginData> getPlugins() {
            return Map.of();
        }

        @Override
        public List<Metadata> resolvePluginLoadOrder(List<Metadata> candidates) {
            return candidates;
        }
    }

    /**
     * Minimal game provider used to construct a {@link LoaderContext}.
     */
    private static final class StubGameProvider implements GameProvider {

        @Override
        public void init() {
        }

        @Override
        public void launch(String[] args) {
        }

        @Override
        public String[] getLaunchArgs() {
            return new String[0];
        }

        @Override
        public String getName() {
            return "Test Game";
        }

        @Override
        public String getId() {
            return "test-game";
        }

        @Override
        public List<String> getAuthors() {
            return List.of();
        }

        @Override
        public String getLicense() {
            return "MIT";
        }

        @Override
        public List<String> getContacts() {
            return List.of();
        }

        @Override
        public String getNormalizedVersion() {
            return "1.0.0";
        }

        @Override
        public String getRawVersion() {
            return "1.0.0";
        }

        @Override
        public Path getLaunchDirectory() {
            return Path.of(".");
        }

        @Override
        public String getEntrypoint() {
            return "test.Main";
        }

        @Override
        public Environment getEnvironment() {
            return Environment.BOTH;
        }
    }
}
