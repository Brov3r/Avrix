package com.avrix.plugins;

import com.avrix.core.Environment;
import com.avrix.core.KnotClassLoader;
import com.avrix.core.LoaderConfig;
import com.avrix.core.Metadata;
import com.avrix.core.MixinRegistrar;
import com.avrix.provider.DefaultLoaderProvider;
import com.avrix.utils.Constants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import test.loader.TestGameProvider;
import test.plugin.PhaseTracker;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;

/**
 * Verifies side-aware entrypoint and mixin lifecycle handling in {@link DefaultPluginManager}.
 */
@DisplayName("DefaultPluginManager side lifecycle tests")
class PluginManagerSideLifecycleTest {

    @TempDir
    Path pluginsDirectory;

    private final List<DefaultPluginManager> managers = new ArrayList<>();
    private final List<KnotClassLoader> classLoaders = new ArrayList<>();

    @BeforeEach
    void resetEvents() {
        PhaseTracker.reset();
    }

    @AfterEach
    void closeManagers() {
        managers.forEach(DefaultPluginManager::close);
        classLoaders.forEach(loader -> {
            try {
                loader.close();
            } catch (IOException ignored) {
                // Test cleanup must not mask lifecycle assertions.
            }
        });
        PhaseTracker.reset();
    }

    /**
     * Verifies that plugins run in dependency order, each plugin runs main handlers before the active side list, and
     * shutdown reverses the full initialization sequence.
     *
     * @throws IOException if a fixture archive cannot be written
     */
    @Test
    @DisplayName("Should initialize main then client handlers in topological order and shut down in reverse")
    void shouldInitializeMainThenActiveSideInTopologicalOrder() throws IOException {
        writePluginJar("core", """
                entrypoints:
                  main:
                    - "%s"
                    - "%s"
                  client:
                    - "%s"
                  server:
                    - "%s"
                """.formatted(
                MainHandler.class.getName(),
                SecondMainHandler.class.getName(),
                ClientHandler.class.getName(),
                ServerHandler.class.getName()
        ), RecordingPlugin.class, MainHandler.class, SecondMainHandler.class, ClientHandler.class, ServerHandler.class);
        writePluginJar("addon", """
                dependencies: {core: "*"}
                entrypoints:
                  main:
                    - "%s"
                  client:
                    - "%s"
                mixins:
                  client:
                    - "example.AddonClientMixin"
                """.formatted(MainHandler.class.getName(), ClientHandler.class.getName()),
                RecordingPlugin.class, MainHandler.class, ClientHandler.class);

        DefaultPluginManager manager = manager(Environment.CLIENT, name -> PhaseTracker.record("mixin:" + name),
                false);
        manager.init();
        manager.loadPlugins();

        assertThat(PhaseTracker.events()).containsExactly(
                "mixin:example.AddonClientMixin",
                "init:core:main",
                "init:core:main-second",
                "init:core:client",
                "init:addon:main",
                "init:addon:client"
        );
        assertThat(manager.getPlugins()).containsKeys("core", "addon");

        PluginData core = manager.getPlugins().get("core");
        assertThat(core.instances()).hasSize(3);
        assertThat(core.instance()).isInstanceOf(MainHandler.class);
        assertThat(core.getPluginInstance()).contains(core.instances().getFirst());

        manager.close();

        assertThat(PhaseTracker.events()).containsExactly(
                "mixin:example.AddonClientMixin",
                "init:core:main",
                "init:core:main-second",
                "init:core:client",
                "init:addon:main",
                "init:addon:client",
                "shutdown:addon:client",
                "shutdown:addon:main",
                "shutdown:core:client",
                "shutdown:core:main-second",
                "shutdown:core:main"
        );
    }

    /**
     * Verifies that a later handler failure rolls back earlier handlers of the same plugin, skips dependents, and lets
     * unrelated plugins load in lenient mode.
     *
     * @throws IOException if a fixture archive cannot be written
     */
    @Test
    @DisplayName("Should roll back a partially initialized plugin and skip its dependents")
    void shouldRollBackPartialPluginAndSkipDependents() throws IOException {
        writePluginJar("failing", """
                loadBefore: ["*"]
                entrypoints:
                  main:
                    - "%s"
                    - "%s"
                  client:
                    - "%s"
                """.formatted(
                MainHandler.class.getName(),
                FailingMainHandler.class.getName(),
                ClientHandler.class.getName()
        ), RecordingPlugin.class, MainHandler.class, FailingMainHandler.class, ClientHandler.class);
        writePluginJar("dependent", """
                dependencies: {failing: "*"}
                """);
        writePluginJar("independent", """
                entrypoints:
                  main:
                    - "%s"
                """.formatted(MainHandler.class.getName()), RecordingPlugin.class, MainHandler.class);

        DefaultPluginManager manager = manager(Environment.CLIENT, name -> {
        }, false);
        manager.init();
        manager.loadPlugins();

        assertThat(PhaseTracker.events()).containsExactly(
                "init:failing:main",
                "init:failing:failing-main",
                "shutdown:failing:failing-main",
                "shutdown:failing:main",
                "init:independent:main"
        );
        assertThat(manager.getPlugins())
                .containsKey("independent")
                .doesNotContainKeys("failing", "dependent");
        assertThat(manager.getLoadResults())
                .extracting(PluginLoadResult::pluginId, PluginLoadResult::status)
                .contains(
                        tuple("failing", PluginLoadStatus.FAILED),
                        tuple("dependent", PluginLoadStatus.SKIPPED),
                        tuple("independent", PluginLoadStatus.LOADED)
                );

        manager.close();

        assertThat(PhaseTracker.events()).containsExactly(
                "init:failing:main",
                "init:failing:failing-main",
                "shutdown:failing:failing-main",
                "shutdown:failing:main",
                "init:independent:main",
                "shutdown:independent:main"
        );
    }

    /**
     * Verifies that strict mode aborts a partial multi-handler plugin after cleaning up exactly the handlers that were
     * constructed.
     *
     * @throws IOException if a fixture archive cannot be written
     */
    @Test
    @DisplayName("Should abort a partial plugin in strict mode after reverse cleanup")
    void shouldAbortPartialPluginInStrictMode() throws IOException {
        writePluginJar("failing", """
                entrypoints:
                  main:
                    - "%s"
                    - "%s"
                """.formatted(MainHandler.class.getName(), FailingMainHandler.class.getName()),
                RecordingPlugin.class, MainHandler.class, FailingMainHandler.class);

        DefaultPluginManager manager = manager(Environment.CLIENT, name -> {
        }, true);
        manager.init();

        assertThatThrownBy(manager::loadPlugins)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Plugin load failed: failing");

        assertThat(PhaseTracker.events()).containsExactly(
                "init:failing:main",
                "init:failing:failing-main",
                "shutdown:failing:failing-main",
                "shutdown:failing:main"
        );
        assertThat(manager.getPlugins()).doesNotContainKey("failing");
    }

    /**
     * Verifies that a direct load cleans up every constructed handler after a partial multi-handler failure.
     *
     * @throws IOException if a fixture archive cannot be written
     */
    @Test
    @DisplayName("Should clean up a partially initialized direct load")
    void shouldCleanUpDirectPartialFailure() throws IOException {
        Metadata metadata = new Metadata.Builder()
                .schema(Constants.METADATA_SCHEMA_2)
                .id("direct-failing")
                .name("Direct failing plugin")
                .version("1.0.0")
                .environment(Environment.BOTH)
                .mainEntrypoints(List.of(MainHandler.class.getName(), FailingMainHandler.class.getName()))
                .clientEntrypoints(List.of(ClientHandler.class.getName()))
                .build();
        Path jarPath = writeClassJar("direct-failing.jar",
                RecordingPlugin.class, MainHandler.class, FailingMainHandler.class, ClientHandler.class);

        DefaultPluginManager manager = manager(Environment.CLIENT, name -> {
        }, false);
        manager.init();

        assertThatThrownBy(() -> manager.loadPlugin(new PluginData(jarPath.toFile(), null, null, metadata)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to load plugin: direct-failing");

        assertThat(PhaseTracker.events()).containsExactly(
                "init:direct-failing:main",
                "init:direct-failing:failing-main",
                "shutdown:direct-failing:failing-main",
                "shutdown:direct-failing:main"
        );
        assertThat(manager.getPlugins()).doesNotContainKey("direct-failing");
        manager.close();
        assertThat(PhaseTracker.events()).containsExactly(
                "init:direct-failing:main",
                "init:direct-failing:failing-main",
                "shutdown:direct-failing:failing-main",
                "shutdown:direct-failing:main"
        );
    }

    /**
     * Verifies that only the active side's handlers and mixins are used and wrong-side classes are never initialized.
     *
     * @throws IOException if a fixture archive cannot be written
     */
    @Test
    @DisplayName("Should ignore wrong-side handlers and mixins")
    void shouldIgnoreWrongSideHandlersAndMixins() throws IOException {
        List<String> validated = new ArrayList<>();
        List<String> added = new ArrayList<>();
        MixinRegistrar registrar = new MixinRegistrar() {
            /** {@inheritDoc} */
            @Override
            public void addMixin(String mixinClass) {
                if (mixinClass.contains("Server")) {
                    throw new IllegalStateException("Wrong-side mixin registered: " + mixinClass);
                }
                added.add(mixinClass);
                PhaseTracker.record("mixin:" + mixinClass);
            }

            /** {@inheritDoc} */
            @Override
            public void validateMixins(Collection<String> mixinClasses) {
                if (mixinClasses.stream().anyMatch(name -> name.contains("Server"))) {
                    throw new IllegalStateException("Wrong-side mixin validated");
                }
                validated.addAll(mixinClasses);
            }
        };

        writePluginJar("sided", """
                entrypoints:
                  main:
                    - "%s"
                  client:
                    - "%s"
                  server:
                    - "%s"
                mixins:
                  common:
                    - "example.CommonMixin"
                  client:
                    - "example.ClientMixin"
                  server:
                    - "example.ServerMixin"
                """.formatted(
                MainHandler.class.getName(),
                ClientHandler.class.getName(),
                WrongServerHandler.class.getName()
        ), RecordingPlugin.class, MainHandler.class, ClientHandler.class, WrongServerHandler.class);

        DefaultPluginManager manager = manager(Environment.CLIENT, registrar, false);
        manager.init();
        manager.loadPlugins();

        assertThat(PhaseTracker.events()).containsExactly(
                "mixin:example.CommonMixin",
                "mixin:example.ClientMixin",
                "init:sided:main",
                "init:sided:client"
        );
        assertThat(validated).containsExactly("example.CommonMixin", "example.ClientMixin");
        assertThat(added).containsExactly("example.CommonMixin", "example.ClientMixin");
        assertThat(manager.getPlugins()).containsKey("sided");
    }

    /**
     * Verifies that a runtime configured as both selects only common handlers and never a concrete side list.
     *
     * @throws IOException if a fixture archive cannot be written
     */
    @Test
    @DisplayName("Should run only common handlers when the runtime environment is both")
    void shouldRunOnlyCommonHandlersWhenRuntimeIsBoth() throws IOException {
        writePluginJar("dual", """
                entrypoints:
                  main:
                    - "%s"
                  client:
                    - "%s"
                  server:
                    - "%s"
                """.formatted(
                MainHandler.class.getName(),
                ClientHandler.class.getName(),
                ServerHandler.class.getName()
        ), RecordingPlugin.class, MainHandler.class, ClientHandler.class, ServerHandler.class);

        DefaultPluginManager manager = manager(Environment.BOTH, name -> {
        }, false);
        manager.init();
        manager.loadPlugins();

        assertThat(PhaseTracker.events()).containsExactly("init:dual:main");
        manager.close();
        assertThat(PhaseTracker.events()).containsExactly("init:dual:main", "shutdown:dual:main");
    }

    /**
     * Verifies that a server runtime runs main handlers followed by the server list.
     *
     * @throws IOException if a fixture archive cannot be written
     */
    @Test
    @DisplayName("Should run main then server handlers on a server runtime")
    void shouldRunMainThenServerHandlers() throws IOException {
        writePluginJar("dual-server", """
                entrypoints:
                  main:
                    - "%s"
                  client:
                    - "%s"
                  server:
                    - "%s"
                """.formatted(
                MainHandler.class.getName(),
                ClientHandler.class.getName(),
                ServerHandler.class.getName()
        ), RecordingPlugin.class, MainHandler.class, ClientHandler.class, ServerHandler.class);

        DefaultPluginManager manager = manager(Environment.SERVER, name -> {
        }, false);
        manager.init();
        manager.loadPlugins();

        assertThat(PhaseTracker.events()).containsExactly("init:dual-server:main", "init:dual-server:server");
        manager.close();
        assertThat(PhaseTracker.events()).containsExactly(
                "init:dual-server:main",
                "init:dual-server:server",
                "shutdown:dual-server:server",
                "shutdown:dual-server:main"
        );
    }

    /**
     * Verifies that a direct load after a batch uses the same side ordering and participates in reverse shutdown.
     *
     * @throws IOException if a fixture archive cannot be written
     */
    @Test
    @DisplayName("Should order and shut down a late direct load with side handlers")
    void shouldOrderAndShutDownLateDirectLoad() throws IOException {
        writePluginJar("batch", """
                entrypoints:
                  main:
                    - "%s"
                  client:
                    - "%s"
                """.formatted(MainHandler.class.getName(), ClientHandler.class.getName()),
                RecordingPlugin.class, MainHandler.class, ClientHandler.class);

        DefaultPluginManager manager = manager(Environment.CLIENT, name -> {
        }, false);
        manager.init();
        manager.loadPlugins();
        assertThat(PhaseTracker.events()).containsExactly("init:batch:main", "init:batch:client");

        Metadata lateMetadata = new Metadata.Builder()
                .schema(Constants.METADATA_SCHEMA_2)
                .id("late")
                .name("Late plugin")
                .version("1.0.0")
                .environment(Environment.BOTH)
                .mainEntrypoints(List.of(SecondMainHandler.class.getName()))
                .clientEntrypoints(List.of(ClientHandler.class.getName()))
                .serverEntrypoints(List.of(WrongServerHandler.class.getName()))
                .build();
        Path lateJar = writeClassJar("late.jar",
                RecordingPlugin.class, SecondMainHandler.class, ClientHandler.class, WrongServerHandler.class);

        manager.loadPlugin(new PluginData(lateJar.toFile(), null, null, lateMetadata));

        assertThat(PhaseTracker.events()).containsExactly(
                "init:batch:main",
                "init:batch:client",
                "init:late:main-second",
                "init:late:client"
        );
        PluginData late = manager.getPlugins().get("late");
        assertThat(late.instances()).hasSize(2);
        assertThat(late.instance()).isInstanceOf(SecondMainHandler.class);
        assertThat(late.getPluginInstance()).contains(late.instances().getFirst());
        assertThat(manager.getLoadResults())
                .extracting(PluginLoadResult::pluginId, PluginLoadResult::status)
                .contains(tuple("batch", PluginLoadStatus.LOADED), tuple("late", PluginLoadStatus.LOADED));

        manager.close();

        assertThat(PhaseTracker.events()).containsExactly(
                "init:batch:main",
                "init:batch:client",
                "init:late:main-second",
                "init:late:client",
                "shutdown:late:client",
                "shutdown:late:main-second",
                "shutdown:batch:client",
                "shutdown:batch:main"
        );
    }

    private DefaultPluginManager manager(Environment environment, MixinRegistrar registrar, boolean strict) {
        LoaderConfig config = LoaderConfig.defaults()
                .withPluginsDirectory(pluginsDirectory)
                .withEnvironment(environment)
                .withStrict(strict);
        KnotClassLoader classLoader = new KnotClassLoader(new URL[0], getClass().getClassLoader());
        classLoaders.add(classLoader);

        DefaultPluginManager manager = new DefaultPluginManager(
                config,
                classLoader,
                new TestGameProvider(),
                new DefaultLoaderProvider(),
                registrar,
                new PluginScanner(config),
                new PluginManifestParser()
        );
        managers.add(manager);
        return manager;
    }

    private Path writePluginJar(String id, String body, Class<?>... classes) throws IOException {
        Files.createDirectories(pluginsDirectory);
        Path jarPath = pluginsDirectory.resolve(id + ".jar");
        String metadata = """
                schema: %d
                id: %s
                name: %s
                version: 1.0.0
                environment: both
                """.formatted(Constants.METADATA_SCHEMA_2, id, id) + body;

        try (JarOutputStream jar = new JarOutputStream(Files.newOutputStream(jarPath))) {
            writeClasses(jar, classes);
            jar.putNextEntry(new JarEntry(Constants.METADATA_NAME));
            jar.write(metadata.getBytes(StandardCharsets.UTF_8));
            jar.closeEntry();
        }
        return jarPath;
    }

    private Path writeClassJar(String name, Class<?>... classes) throws IOException {
        Files.createDirectories(pluginsDirectory);
        Path jarPath = pluginsDirectory.resolve(name);
        try (JarOutputStream jar = new JarOutputStream(Files.newOutputStream(jarPath))) {
            writeClasses(jar, classes);
        }
        return jarPath;
    }

    private static void writeClasses(JarOutputStream jar, Class<?>... classes) throws IOException {
        for (Class<?> type : classes) {
            String resource = type.getName().replace('.', '/') + ".class";
            try (InputStream input = type.getClassLoader().getResourceAsStream(resource)) {
                if (input == null) {
                    throw new IllegalStateException("Class resource not found: " + resource);
                }
                jar.putNextEntry(new JarEntry(resource));
                jar.write(input.readAllBytes());
                jar.closeEntry();
            }
        }
    }

    /**
     * Common recorder that derives lifecycle labels from the owning plugin ID and the handler phase.
     */
    public abstract static class RecordingPlugin implements Plugin {
        private String pluginId;

        /**
         * Returns the handler phase used in recorded lifecycle events.
         *
         * @return phase label
         */
        protected abstract String phase();

        /** {@inheritDoc} */
        @Override
        public void onInitialize(PluginData pluginData) {
            pluginId = pluginData.id();
            PhaseTracker.record("init:" + pluginId + ":" + phase());
        }

        /** {@inheritDoc} */
        @Override
        public void onShutdown() {
            PhaseTracker.record("shutdown:" + pluginId + ":" + phase());
        }
    }

    /** Records a main handler. */
    public static final class MainHandler extends RecordingPlugin {
        /** {@inheritDoc} */
        @Override
        protected String phase() {
            return "main";
        }
    }

    /** Records a second main handler. */
    public static final class SecondMainHandler extends RecordingPlugin {
        /** {@inheritDoc} */
        @Override
        protected String phase() {
            return "main-second";
        }
    }

    /** Records a client handler. */
    public static final class ClientHandler extends RecordingPlugin {
        /** {@inheritDoc} */
        @Override
        protected String phase() {
            return "client";
        }
    }

    /** Records a server handler. */
    public static final class ServerHandler extends RecordingPlugin {
        /** {@inheritDoc} */
        @Override
        protected String phase() {
            return "server";
        }
    }

    /** Fails after recording a main handler initialization. */
    public static final class FailingMainHandler extends RecordingPlugin {
        /** {@inheritDoc} */
        @Override
        protected String phase() {
            return "failing-main";
        }

        /** {@inheritDoc} */
        @Override
        public void onInitialize(PluginData pluginData) {
            super.onInitialize(pluginData);
            throw new IllegalStateException("Simulated main handler failure");
        }
    }

    /** Server handler that fails the test if it is ever initialized on a client run. */
    public static final class WrongServerHandler extends RecordingPlugin {
        static {
            PhaseTracker.record("loaded:wrong-server");
            failIfLoaded();
        }

        /**
         * Aborts the test process when the wrong-side class is initialized.
         */
        private static void failIfLoaded() {
            throw new AssertionError("Wrong-side server handler was loaded");
        }

        /** {@inheritDoc} */
        @Override
        protected String phase() {
            return "wrong-server";
        }
    }
}
