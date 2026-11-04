package com.avrix.plugins;

import com.avrix.core.Environment;
import com.avrix.core.KnotClassLoader;
import com.avrix.core.LoaderConfig;
import com.avrix.core.Metadata;
import com.avrix.core.MixinRegistrar;
import com.avrix.provider.DefaultLoaderProvider;
import com.avrix.provider.GameProvider;
import com.avrix.provider.LoaderProvider;
import com.avrix.utils.Constants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import test.plugin.ErrorThrowingPlugin;
import test.plugin.LifecyclePlugin;
import test.plugin.PhaseTracker;
import test.plugin.ShutdownFailingPlugin;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.groups.Tuple.tuple;

/**
 * Edge-case unit tests for {@link DefaultPluginManager} failure handling, lifecycle, and reporting.
 */
@DisplayName("DefaultPluginManager Edge Case Tests")
class DefaultPluginManagerEdgeTest {

    private static final int SCHEMA = Constants.METADATA_SCHEMA;

    @TempDir
    Path tempRoot;

    private final MixinRegistrar mixinRegistrar = mixinClass -> {
    };

    private Path pluginsDirectory;
    private KnotClassLoader classLoader;
    private ClassLoader originalContextClassLoader;

    @BeforeEach
    void setUp() {
        PhaseTracker.reset();
        originalContextClassLoader = Thread.currentThread().getContextClassLoader();
        pluginsDirectory = tempRoot.resolve("plugins");
        classLoader = new KnotClassLoader(new URL[0], ClassLoader.getSystemClassLoader());
        Thread.currentThread().setContextClassLoader(classLoader);
    }

    @AfterEach
    void tearDown() throws IOException {
        try {
            if (classLoader != null) {
                classLoader.close();
            }
        } finally {
            PhaseTracker.reset();
            Thread.currentThread().setContextClassLoader(originalContextClassLoader);
        }
    }

    @Nested
    @DisplayName("Metadata Parse Failures")
    class MetadataFailureTests {

        @Test
        @DisplayName("Should abort in strict mode and report the corrupt archive as failed")
        void shouldAbortOnCorruptMetadataInStrictMode() throws IOException {
            DefaultPluginManager manager = createManager(true);
            manager.init();

            Files.writeString(pluginsDirectory.resolve("corrupt-plugin.jar"), "NOT_A_ZIP_ARCHIVE");

            assertThatThrownBy(manager::loadPlugins)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Failed to parse plugin metadata: corrupt-plugin.jar");

            assertThat(manager.getLoadResults())
                    .extracting(PluginLoadResult::pluginId, PluginLoadResult::status)
                    .contains(tuple("corrupt-plugin.jar", PluginLoadStatus.FAILED));
        }

        @Test
        @DisplayName("Should report a corrupt archive as failed and still load valid plugins in non-strict mode")
        void shouldContinueAfterCorruptMetadataInNonStrictMode() throws IOException {
            DefaultPluginManager manager = createManager(false);
            manager.init();

            Files.writeString(pluginsDirectory.resolve("corrupt-plugin.jar"), "NOT_A_ZIP_ARCHIVE");
            createPluginJar(pluginsDirectory.resolve("valid-plugin.jar"), "valid-plugin", "");

            manager.loadPlugins();

            assertThat(manager.getPlugins()).containsKey("valid-plugin");
            assertThat(manager.getLoadResults())
                    .extracting(PluginLoadResult::pluginId, PluginLoadResult::status)
                    .contains(
                            tuple("corrupt-plugin.jar", PluginLoadStatus.FAILED),
                            tuple("valid-plugin", PluginLoadStatus.LOADED)
                    );
        }
    }

    @Nested
    @DisplayName("Dependency Resolver Failures")
    class ResolverFailureTests {

        @Test
        @DisplayName("Should abort in strict mode on duplicate plugin identifiers")
        void shouldAbortOnDuplicateIdentifiersInStrictMode() throws IOException {
            DefaultPluginManager manager = createManager(true);
            manager.init();

            createPluginJar(pluginsDirectory.resolve("first.jar"), "duplicate-plugin", "");
            createPluginJar(pluginsDirectory.resolve("second.jar"), "duplicate-plugin", "");

            assertThatThrownBy(manager::loadPlugins)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Failed to resolve plugin load order");

            assertThat(manager.getLoadResults())
                    .hasSize(2)
                    .extracting(PluginLoadResult::status)
                    .containsOnly(PluginLoadStatus.FAILED);
            assertThat(manager.getPlugins()).doesNotContainKey("duplicate-plugin");
        }

        @Test
        @DisplayName("Should abort in strict mode when a user plugin uses a reserved core identifier")
        void shouldAbortOnReservedIdentifierInStrictMode() throws IOException {
            DefaultPluginManager manager = createManager(true);
            manager.init();

            createPluginJar(
                    pluginsDirectory.resolve("reserved.jar"),
                    Constants.DEFAULT_GAME_PROVIDER_ID,
                    ""
            );

            Throwable thrown = catchThrowable(manager::loadPlugins);

            assertThat(thrown)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Failed to resolve plugin load order");
            assertThat(thrown.getCause())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("reserved for a core component");
            assertThat(manager.getLoadResults())
                    .extracting(PluginLoadResult::pluginId, PluginLoadResult::status)
                    .contains(tuple(Constants.DEFAULT_GAME_PROVIDER_ID, PluginLoadStatus.FAILED));
        }

        @Test
        @DisplayName("Should report every candidate as failed and load nothing on resolver failure in non-strict mode")
        void shouldFailAllCandidatesOnResolverFailureInNonStrictMode() throws IOException {
            DefaultPluginManager manager = createManager(false);
            manager.init();

            createPluginJar(pluginsDirectory.resolve("first.jar"), "duplicate-plugin", "");
            createPluginJar(pluginsDirectory.resolve("second.jar"), "duplicate-plugin", "");
            createPluginJar(pluginsDirectory.resolve("third.jar"), "independent-plugin", "");

            manager.loadPlugins();

            assertThat(manager.getLoadResults())
                    .hasSize(3)
                    .extracting(PluginLoadResult::status)
                    .containsOnly(PluginLoadStatus.FAILED);
            assertThat(manager.getPlugins())
                    .containsOnlyKeys("dummy-game", Constants.LOADER_ID);
        }
    }

    @Nested
    @DisplayName("Lifecycle & Close Semantics")
    class LifecycleTests {

        @Test
        @DisplayName("Should make close idempotent and keep shutting down plugins after one throws")
        void shouldCloseIdempotentlyAndIsolateShutdownFailures() throws IOException {
            DefaultPluginManager manager = createManager(false);
            manager.init();

            createPluginJar(
                    pluginsDirectory.resolve("lifecycle-plugin.jar"),
                    "lifecycle-plugin",
                    LifecyclePlugin.class.getName(),
                    LifecyclePlugin.class
            );
            createPluginJar(
                    pluginsDirectory.resolve("shutdown-failing-plugin.jar"),
                    "shutdown-failing-plugin",
                    ShutdownFailingPlugin.class.getName(),
                    ShutdownFailingPlugin.class
            );

            manager.loadPlugins();
            manager.close();

            assertThat(PhaseTracker.events()).containsExactly(
                    "init:lifecycle",
                    "init:shutdown-failing",
                    "shutdown:shutdown-failing",
                    "shutdown:lifecycle"
            );

            manager.close();

            assertThat(PhaseTracker.events()).containsExactly(
                    "init:lifecycle",
                    "init:shutdown-failing",
                    "shutdown:shutdown-failing",
                    "shutdown:lifecycle"
            );
            assertThat(manager.getPlugins()).isEmpty();
        }

        @Test
        @DisplayName("Should ignore a duplicate loadPlugin call after the first successful load")
        void shouldIgnoreDuplicateLoadPluginAfterSuccess() throws IOException {
            DefaultPluginManager manager = createManager(false);
            manager.init();

            Path jarPath = pluginsDirectory.resolve("lifecycle-plugin.jar");
            createPluginJar(
                    jarPath,
                    "lifecycle-plugin",
                    LifecyclePlugin.class.getName(),
                    LifecyclePlugin.class
            );

            Metadata containerMetadata = new Metadata.Builder()
                    .schema(SCHEMA)
                    .id("lifecycle-plugin")
                    .name("Lifecycle plugin")
                    .version("1.0.0")
                    .environment(Environment.BOTH)
                    .entrypoint(LifecyclePlugin.class.getName())
                    .build();

            PluginData container = new PluginData(jarPath.toFile(), null, null, containerMetadata);

            manager.loadPlugin(container);
            manager.loadPlugin(container);

            assertThat(PhaseTracker.events()).containsExactly("init:lifecycle");
        }

        @Test
        @DisplayName("Should reject loadPlugins and loadPlugin after close")
        void shouldRejectLoadingAfterClose() {
            DefaultPluginManager manager = createManager(false);
            manager.init();
            manager.close();

            assertThatThrownBy(manager::loadPlugins)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("PluginManager is closed");

            assertThatThrownBy(() -> manager.loadPlugin(new PluginData(createMeta("late-plugin", "1.0.0"))))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("PluginManager is closed");
        }
    }

    @Nested
    @DisplayName("Load Reporting & Disabled Filters")
    class ReportingTests {

        @Test
        @DisplayName("Should match disabled.txt entries case-insensitively by ID and archive name")
        void shouldMatchDisabledEntriesCaseInsensitively() throws IOException {
            DefaultPluginManager manager = createManager(false);
            manager.init();

            createPluginJar(pluginsDirectory.resolve("id-disabled.jar"), "upper-id", "");
            createPluginJar(pluginsDirectory.resolve("name-disabled.JAR"), "name-upper", "");
            createPluginJar(pluginsDirectory.resolve("enabled-plugin.jar"), "enabled-plugin", "");

            Files.writeString(pluginsDirectory.resolve(Constants.DISABLED_PLUGINS_FILE_NAME), """
                    # Disabled entries are case-insensitive
                    UPPER-ID
                    NAME-DISABLED.JAR
                    """);

            manager.loadPlugins();

            assertThat(manager.getPlugins())
                    .containsKey("enabled-plugin")
                    .doesNotContainKeys("upper-id", "name-upper");
            assertThat(manager.getLoadResults())
                    .extracting(PluginLoadResult::pluginId, PluginLoadResult::status)
                    .contains(
                            tuple("upper-id", PluginLoadStatus.SKIPPED),
                            tuple("name-disabled.JAR", PluginLoadStatus.SKIPPED),
                            tuple("enabled-plugin", PluginLoadStatus.LOADED)
                    );
        }

        @Test
        @DisplayName("Should report LOADED, FAILED, and SKIPPED outcomes in a single run")
        void shouldReportAllLoadStatuses() throws IOException {
            DefaultPluginManager manager = createManager(false);
            manager.init();

            createPluginJar(pluginsDirectory.resolve("valid-plugin.jar"), "valid-plugin", "");
            createPluginJar(
                    pluginsDirectory.resolve("failing-plugin.jar"),
                    "failing-plugin",
                    ErrorThrowingPlugin.class.getName(),
                    ErrorThrowingPlugin.class
            );
            createPluginJar(pluginsDirectory.resolve("skipped-plugin.jar"), "skipped-plugin", "");

            Files.writeString(pluginsDirectory.resolve(Constants.DISABLED_PLUGINS_FILE_NAME), "skipped-plugin\n");

            manager.loadPlugins();

            assertThat(manager.getPlugins())
                    .containsKey("valid-plugin")
                    .doesNotContainKeys("failing-plugin", "skipped-plugin");
            assertThat(manager.getLoadResults())
                    .extracting(PluginLoadResult::pluginId, PluginLoadResult::status)
                    .contains(
                            tuple("valid-plugin", PluginLoadStatus.LOADED),
                            tuple("failing-plugin", PluginLoadStatus.FAILED),
                            tuple("skipped-plugin", PluginLoadStatus.SKIPPED)
                    );
        }
    }

    private DefaultPluginManager createManager(boolean strict) {
        LoaderConfig config = new LoaderConfig(
                pluginsDirectory,
                Constants.METADATA_NAME,
                Constants.PLUGIN_EXTENSION,
                Constants.PLUGIN_ICON_NAME,
                Constants.DEFAULT_GAME_PROVIDER_ID,
                Environment.BOTH,
                true,
                strict
        );

        GameProvider gameProvider = new DummyGameProvider();
        LoaderProvider loaderProvider = new DefaultLoaderProvider();

        return new DefaultPluginManager(
                config,
                classLoader,
                gameProvider,
                loaderProvider,
                mixinRegistrar,
                new PluginScanner(config),
                new PluginManifestParser(config.strict())
        );
    }

    private Metadata createMeta(String id, String version) {
        return new Metadata.Builder()
                .schema(SCHEMA)
                .id(id)
                .name(id + " plugin")
                .version(version)
                .environment(Environment.BOTH)
                .build();
    }

    /**
     * Creates a plugin archive containing an optional entrypoint class and generated metadata.
     *
     * @param destination target JAR path
     * @param id          plugin identifier written to the manifest
     * @param entrypoint  entrypoint class name, or blank for a library-only plugin
     * @param classes     classes to copy into the archive
     * @throws IOException if the archive cannot be written
     */
    private void createPluginJar(Path destination, String id, String entrypoint, Class<?>... classes) throws IOException {
        Files.createDirectories(destination.getParent());

        try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(destination))) {
            for (Class<?> clazz : classes) {
                String classPath = clazz.getName().replace('.', '/') + ".class";
                try (var classBytes = clazz.getClassLoader().getResourceAsStream(classPath)) {
                    if (classBytes == null) {
                        throw new IllegalStateException("Class resource not found: " + classPath);
                    }
                    jos.putNextEntry(new JarEntry(classPath));
                    jos.write(classBytes.readAllBytes());
                    jos.closeEntry();
                }
            }

            StringBuilder yaml = new StringBuilder("""
                    schema: %d
                    id: %s
                    name: %s
                    version: 1.0.0
                    environment: both
                    """.formatted(SCHEMA, id, id + " plugin"));

            if (entrypoint != null && !entrypoint.isBlank()) {
                yaml.append("entrypoint: ").append(entrypoint).append('\n');
            }

            jos.putNextEntry(new JarEntry(Constants.METADATA_NAME));
            jos.write(yaml.toString().getBytes(StandardCharsets.UTF_8));
            jos.closeEntry();
        }
    }

    /**
     * Minimal game provider used to register a core descriptor during manager initialization.
     */
    private static final class DummyGameProvider implements GameProvider {

        /** {@inheritDoc} */
        @Override
        public String getId() {
            return "dummy-game";
        }

        /** {@inheritDoc} */
        @Override
        public void init() {
        }

        /** {@inheritDoc} */
        @Override
        public void launch(String[] args) {
        }

        /** {@inheritDoc} */
        @Override
        public String[] getLaunchArgs() {
            return new String[0];
        }

        /** {@inheritDoc} */
        @Override
        public String getName() {
            return "Dummy Game";
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
            return Paths.get(".").toAbsolutePath().normalize();
        }

        /** {@inheritDoc} */
        @Override
        public Environment getEnvironment() {
            return Environment.BOTH;
        }

        /** {@inheritDoc} */
        @Override
        public String getEntrypoint() {
            return "com.dummy.Main";
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
    }
}
