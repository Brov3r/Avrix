package com.avrix.loader;

import com.avrix.core.Environment;
import com.avrix.core.KnotClassLoader;
import com.avrix.core.Metadata;
import com.avrix.core.ServiceManager;
import com.avrix.plugins.PluginData;
import com.avrix.plugins.PluginManager;
import com.avrix.api.GameProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
        LoaderContext context = new LoaderContext(classLoader, new StubGameProvider(), null, pluginManager);

        assertThatCode(context::close).doesNotThrowAnyException();

        assertThat(pluginManager.closeCalls).isEqualTo(1);
        assertThat(classLoader.closeCalls).isEqualTo(1);
    }

    @Test
    @DisplayName("Should unregister loader services when the classloader close fails with an unchecked error")
    void shouldUnregisterServicesWhenClassLoaderCloseFailsWithUncheckedError() {
        CountingClassLoader classLoader = new CountingClassLoader();
        classLoader.uncheckedFailure = new IllegalStateException("classloader unchecked shutdown failure");
        RecordingPluginManager pluginManager = new RecordingPluginManager();
        StubGameProvider gameProvider = new StubGameProvider();
        LoaderContext context = new LoaderContext(classLoader, gameProvider, null, pluginManager);
        ServiceManager.clear();
        ServiceManager.register(KnotClassLoader.class, classLoader);
        ServiceManager.register(PluginManager.class, pluginManager);
        ServiceManager.register(GameProvider.class, gameProvider);

        try {
            assertThatCode(context::close).doesNotThrowAnyException();

            assertThat(ServiceManager.contains(KnotClassLoader.class)).isFalse();
            assertThat(ServiceManager.contains(PluginManager.class)).isFalse();
            assertThat(ServiceManager.contains(GameProvider.class)).isFalse();
            assertThat(classLoader.closeCalls).isEqualTo(1);
        } finally {
            ServiceManager.clear();
        }
    }

    @Test
    @DisplayName("Should rethrow a fatal classloader close failure")
    void shouldRethrowFatalClassLoaderCloseFailure() {
        CountingClassLoader classLoader = new CountingClassLoader();
        SimulatedVmFailure fatal = new SimulatedVmFailure();
        classLoader.uncheckedFailure = fatal;
        LoaderContext context = new LoaderContext(classLoader, new StubGameProvider(), null, new RecordingPluginManager());

        assertThatThrownBy(context::close).isSameAs(fatal);
    }

    @Test
    @DisplayName("Should tolerate an IOException from the classloader close")
    void shouldTolerateClassLoaderCloseFailure() {
        CountingClassLoader classLoader = new CountingClassLoader();
        classLoader.failure = new IOException("classloader shutdown failure");
        LoaderContext context = new LoaderContext(classLoader, new StubGameProvider(), null, new RecordingPluginManager());

        assertThatCode(context::close).doesNotThrowAnyException();

        assertThat(classLoader.closeCalls).isEqualTo(1);
    }

    @Test
    @DisplayName("Should close each dependency only once")
    void shouldCloseEachDependencyOnce() {
        CountingClassLoader classLoader = new CountingClassLoader();
        RecordingPluginManager pluginManager = new RecordingPluginManager();
        LoaderContext context = new LoaderContext(classLoader, new StubGameProvider(), null, pluginManager);

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
        LoaderContext context = new LoaderContext(classLoader, new StubGameProvider(), null, new RecordingPluginManager());

        assertThatCode(context::close).doesNotThrowAnyException();

        assertThat(classLoader.closeCalls).isEqualTo(2);
    }

    /**
     * Classloader that records close invocations and can simulate a shutdown failure.
     */
    private static final class CountingClassLoader extends KnotClassLoader {

        private int closeCalls;
        private IOException failure;
        private Throwable uncheckedFailure;

        private CountingClassLoader() {
            super(new URL[0], LoaderContextEdgeTest.class.getClassLoader());
        }

        @Override
        public void close() throws IOException {
            closeCalls++;
            if (failure != null) {
                throw failure;
            }
            if (uncheckedFailure instanceof RuntimeException runtimeFailure) {
                throw runtimeFailure;
            }
            if (uncheckedFailure instanceof Error errorFailure) {
                throw errorFailure;
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

    /**
     * Simulated virtual machine failure used to verify fatal error propagation.
     */
    private static final class SimulatedVmFailure extends VirtualMachineError {
        private SimulatedVmFailure() {
            super("simulated fatal classloader shutdown failure");
        }
    }
}
