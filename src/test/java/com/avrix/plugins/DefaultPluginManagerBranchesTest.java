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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import test.loader.TestGameProvider;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.groups.Tuple.tuple;

/**
 * Branch-focused unit tests for {@link DefaultPluginManager} internal preparation, startup, and close paths.
 */
@DisplayName("DefaultPluginManager Branch Tests")
class DefaultPluginManagerBranchesTest {

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
            Thread.currentThread().setContextClassLoader(originalContextClassLoader);
        }
    }

    @Test
    @DisplayName("Should initialize when the plugins directory already exists")
    void shouldInitializeWhenPluginsDirectoryAlreadyExists() throws IOException {
        Files.createDirectories(pluginsDirectory);
        DefaultPluginManager manager = createManager(pluginsDirectory, false, Constants.PLUGIN_ICON_NAME);

        manager.init();

        assertThat(manager.getPlugins()).containsKeys(TestGameProvider.ID, Constants.LOADER_ID);
    }

    @Test
    @DisplayName("Should reject initialization when the plugins path is a regular file")
    void shouldRejectInitializationWhenPluginsPathIsFile() throws IOException {
        Path pluginsFile = tempRoot.resolve("plugins-as-file");
        Files.writeString(pluginsFile, "not a directory");
        DefaultPluginManager manager = createManager(pluginsFile, false, Constants.PLUGIN_ICON_NAME);

        assertThatThrownBy(manager::init)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("exists but is not a directory");
    }

    @Test
    @DisplayName("Should wrap directory creation failures in UncheckedIOException")
    void shouldWrapDirectoryCreationFailure() throws IOException {
        Path blockingFile = tempRoot.resolve("blocking-parent");
        Files.writeString(blockingFile, "blocking file");
        Path nestedDirectory = blockingFile.resolve("plugins");
        DefaultPluginManager manager = createManager(nestedDirectory, false, Constants.PLUGIN_ICON_NAME);

        assertThatThrownBy(manager::init)
                .isInstanceOf(UncheckedIOException.class)
                .hasMessageContaining("Failed to create plugins directory");
    }

    @Test
    @DisplayName("Should allow initialization retry after a directory creation failure")
    void shouldAllowInitializationRetryAfterDirectoryFailure() throws IOException {
        Path blocker = tempRoot.resolve("retry-blocker");
        Files.writeString(blocker, "blocking file");
        Path nestedPlugins = blocker.resolve("plugins");
        DefaultPluginManager manager = createManager(nestedPlugins, false, Constants.PLUGIN_ICON_NAME);

        assertThatThrownBy(manager::init)
                .isInstanceOf(UncheckedIOException.class);

        Files.delete(blocker);
        Files.createDirectories(blocker);
        manager.init();
        manager.loadPlugins();

        assertThat(manager.getPlugins()).containsKeys(TestGameProvider.ID, Constants.LOADER_ID);
    }

    @Test
    @DisplayName("Should skip core metadata returned by the load-order resolver")
    void shouldSkipCoreMetadataReturnedByResolver() throws IOException {
        LoaderConfig config = config(pluginsDirectory, false, Constants.PLUGIN_ICON_NAME);
        LoaderProvider loaderProvider = new DefaultLoaderProvider();
        Metadata coreMetadata = loaderProvider.getMetadata();

        DefaultPluginManager manager = new DefaultPluginManager(
                config,
                classLoader,
                new TestGameProvider(),
                loaderProvider,
                mixinRegistrar,
                new PluginScanner(config),
                new PluginManifestParser()
        ) {
            @Override
            public List<Metadata> resolvePluginLoadOrder(List<Metadata> candidates) {
                return List.of(coreMetadata);
            }
        };

        manager.init();
        createPluginJar(pluginsDirectory.resolve("real-plugin.jar"), "real-plugin");

        manager.loadPlugins();

        assertThat(manager.getLoadResults()).isEmpty();
        assertThat(manager.getPlugins())
                .containsKeys(TestGameProvider.ID, Constants.LOADER_ID)
                .doesNotContainKey("real-plugin");
    }

    @Test
    @DisplayName("Should not enqueue an already loaded plugin during a later scan")
    void shouldNotEnqueueAlreadyLoadedPlugin() throws IOException {
        DefaultPluginManager manager = createManager(pluginsDirectory, false, Constants.PLUGIN_ICON_NAME);
        manager.init();

        Path directJar = createEmptyJar(tempRoot.resolve("direct-already-loaded.jar"));
        Metadata metadata = createMeta("already-loaded", "1.0.0");
        manager.loadPlugin(new PluginData(directJar.toFile(), null, null, metadata));

        createPluginJar(pluginsDirectory.resolve("already-loaded.jar"), "already-loaded");
        manager.loadPlugins();

        assertThat(manager.getLoadResults()).isEmpty();
        assertThat(manager.getPlugins().get("already-loaded").getPluginFile())
                .contains(directJar.toFile());
    }

    @Test
    @DisplayName("Should report preparation failures without a physical JAR and continue")
    void shouldReportPreparationFailureWithoutPhysicalJar() throws IOException {
        LoaderConfig config = config(pluginsDirectory, false, Constants.PLUGIN_ICON_NAME);
        Metadata ghost = createMeta("ghost-plugin", "1.0.0");
        DefaultPluginManager manager = managerReturning(config, ghost);

        manager.init();
        createPluginJar(pluginsDirectory.resolve("real-plugin.jar"), "real-plugin");

        manager.loadPlugins();

        assertThat(manager.getLoadResults())
                .extracting(PluginLoadResult::pluginId, PluginLoadResult::status)
                .contains(tuple("ghost-plugin", PluginLoadStatus.FAILED));
        assertThat(manager.getPlugins()).doesNotContainKey("ghost-plugin");
    }

    @Test
    @DisplayName("Should abort preparation failures in strict mode")
    void shouldAbortOnPreparationFailureInStrictMode() throws IOException {
        LoaderConfig config = config(pluginsDirectory, true, Constants.PLUGIN_ICON_NAME);
        Metadata ghost = createMeta("ghost-plugin", "1.0.0");
        DefaultPluginManager manager = managerReturning(config, ghost);

        manager.init();
        createPluginJar(pluginsDirectory.resolve("real-plugin.jar"), "real-plugin");

        assertThatThrownBy(manager::loadPlugins)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Plugin preparation failed: ghost-plugin");
        assertThat(manager.getLoadResults())
                .extracting(PluginLoadResult::pluginId, PluginLoadResult::status)
                .contains(tuple("ghost-plugin", PluginLoadStatus.FAILED));
    }

    @Test
    @DisplayName("Should reject loadPlugin before initialization")
    void shouldRejectLoadPluginBeforeInitialization() {
        DefaultPluginManager manager = createManager(pluginsDirectory, false, Constants.PLUGIN_ICON_NAME);
        PluginData container = new PluginData(createMeta("late-plugin", "1.0.0"));

        assertThatThrownBy(() -> manager.loadPlugin(container))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("PluginManager is not initialized. Call init() first.");
    }

    @Test
    @DisplayName("Should report an already loaded plugin as not prepared again")
    void shouldReportAlreadyLoadedPluginAsNotPrepared() throws Exception {
        DefaultPluginManager manager = createManager(pluginsDirectory, false, Constants.PLUGIN_ICON_NAME);
        manager.init();

        Path jar = createEmptyJar(tempRoot.resolve("loaded-plugin.jar"));
        PluginData container = new PluginData(jar.toFile(), null, null, createMeta("loaded-plugin", "1.0.0"));
        manager.loadPlugin(container);

        Object prepared = invoke(manager, "preparePlugin", new Class<?>[]{PluginData.class}, container);

        assertThat(prepared).isEqualTo(false);
    }

    @Test
    @DisplayName("Should report an already prepared plugin as ready")
    void shouldReportAlreadyPreparedPluginAsReady() throws Exception {
        DefaultPluginManager manager = createManager(pluginsDirectory, false, Constants.PLUGIN_ICON_NAME);
        manager.init();

        Path jar = createEmptyJar(tempRoot.resolve("prepared-plugin.jar"));
        PluginData container = new PluginData(jar.toFile(), null, null, createMeta("prepared-plugin", "1.0.0"));

        Object first = invoke(manager, "preparePlugin", new Class<?>[]{PluginData.class}, container);
        Object second = invoke(manager, "preparePlugin", new Class<?>[]{PluginData.class}, container);

        assertThat(first).isEqualTo(true);
        assertThat(second).isEqualTo(true);
    }

    @Test
    @DisplayName("Should reject preparing a plugin without a physical JAR")
    void shouldRejectPrepareWithoutPhysicalJar() throws Exception {
        DefaultPluginManager manager = createManager(pluginsDirectory, false, Constants.PLUGIN_ICON_NAME);
        manager.init();
        PluginData container = new PluginData(createMeta("prepare-without-jar", "1.0.0"));

        Throwable thrown = catchThrowable(() ->
                invoke(manager, "preparePlugin", new Class<?>[]{PluginData.class}, container));

        assertThat(thrown)
                .isInstanceOf(InvocationTargetException.class)
                .hasCauseInstanceOf(IllegalStateException.class);
        assertThat(thrown.getCause())
                .hasMessageContaining("without a physical JAR file");
    }

    @Test
    @DisplayName("Should ignore startup for an already loaded plugin")
    void shouldIgnoreStartupForAlreadyLoadedPlugin() throws Exception {
        DefaultPluginManager manager = createManager(pluginsDirectory, false, Constants.PLUGIN_ICON_NAME);
        manager.init();

        Path jar = createEmptyJar(tempRoot.resolve("start-loaded.jar"));
        PluginData container = new PluginData(jar.toFile(), null, null, createMeta("start-loaded", "1.0.0"));
        manager.loadPlugin(container);

        invoke(manager, "startPlugin", new Class<?>[]{PluginData.class}, container);

        assertThat(manager.getPlugins()).containsKey("start-loaded");
    }

    @Test
    @DisplayName("Should reject starting a plugin that was never prepared")
    void shouldRejectStartupForUnpreparedPlugin() throws Exception {
        DefaultPluginManager manager = createManager(pluginsDirectory, false, Constants.PLUGIN_ICON_NAME);
        manager.init();
        PluginData container = new PluginData(createMeta("unprepared-plugin", "1.0.0"));

        Throwable thrown = catchThrowable(() ->
                invoke(manager, "startPlugin", new Class<?>[]{PluginData.class}, container));

        assertThat(thrown)
                .isInstanceOf(InvocationTargetException.class)
                .hasCauseInstanceOf(IllegalStateException.class);
        assertThat(thrown.getCause())
                .hasMessage("Plugin was not prepared: unprepared-plugin");
    }

    @Test
    @DisplayName("Should reject starting a prepared plugin without a physical JAR")
    void shouldRejectStartupWithoutPhysicalJarWhenMarkedPrepared() throws Exception {
        DefaultPluginManager manager = createManager(pluginsDirectory, false, Constants.PLUGIN_ICON_NAME);
        manager.init();
        Metadata metadata = createMeta("prepared-without-jar", "1.0.0");
        PluginData container = new PluginData(metadata);
        preparedPluginIds(manager).add(metadata.id());

        Throwable thrown = catchThrowable(() ->
                invoke(manager, "startPlugin", new Class<?>[]{PluginData.class}, container));

        assertThat(thrown)
                .isInstanceOf(InvocationTargetException.class)
                .hasCauseInstanceOf(IllegalStateException.class);
        assertThat(thrown.getCause())
                .hasMessageContaining("without a physical JAR file");
    }

    @Test
    @DisplayName("Should keep closing when the load order references an unknown plugin")
    void shouldKeepClosingForUnknownPluginInLoadOrder() throws Exception {
        DefaultPluginManager manager = createManager(pluginsDirectory, false, Constants.PLUGIN_ICON_NAME);
        manager.init();
        loadedPluginOrder(manager).add("phantom-plugin");

        manager.close();

        assertThat(manager.getPlugins()).isEmpty();
    }

    @Test
    @DisplayName("Should detect a disabled archive by file name in the private helper")
    void shouldDetectDisabledArchiveByFileName() throws Exception {
        Metadata metadata = createMeta("archive-plugin", "1.0.0");
        Set<String> disabledPlugins = Set.of("archive-disabled.jar");
        Method method = DefaultPluginManager.class.getDeclaredMethod(
                "isPluginDisabled",
                Set.class,
                Metadata.class,
                File.class
        );
        method.setAccessible(true);

        assertThat(method.invoke(null, disabledPlugins, metadata, new File("archive-disabled.jar")))
                .isEqualTo(true);
        assertThat(method.invoke(null, disabledPlugins, metadata, new File("other.jar")))
                .isEqualTo(false);
    }

    @Test
    @DisplayName("Should return null icon URI when the plugin file is null")
    void shouldReturnNullIconUriForNullFile() throws Exception {
        DefaultPluginManager manager = createManager(pluginsDirectory, false, Constants.PLUGIN_ICON_NAME);

        Object iconUri = invoke(manager, "buildIconUri", new Class<?>[]{File.class}, new Object[]{null});

        assertThat(iconUri).isNull();
    }

    @Test
    @DisplayName("Should return null icon URI when the configured icon name cannot form a URI")
    void shouldReturnNullIconUriForInvalidIconName() throws Exception {
        DefaultPluginManager manager = createManager(pluginsDirectory, false, "icon name.png");

        Object iconUri = invoke(
                manager,
                "buildIconUri",
                new Class<?>[]{File.class},
                tempRoot.resolve("plugin.jar").toFile()
        );

        assertThat(iconUri).isNull();
    }

    private DefaultPluginManager managerReturning(LoaderConfig config, Metadata metadata) {
        return new DefaultPluginManager(
                config,
                classLoader,
                new TestGameProvider(),
                new DefaultLoaderProvider(),
                mixinRegistrar,
                new PluginScanner(config),
                new PluginManifestParser()
        ) {
            @Override
            public List<Metadata> resolvePluginLoadOrder(List<Metadata> candidates) {
                return List.of(metadata);
            }
        };
    }

    private DefaultPluginManager createManager(Path directory, boolean strict, String iconName) {
        LoaderConfig config = config(directory, strict, iconName);
        GameProvider gameProvider = new TestGameProvider();
        LoaderProvider loaderProvider = new DefaultLoaderProvider();

        return new DefaultPluginManager(
                config,
                classLoader,
                gameProvider,
                loaderProvider,
                mixinRegistrar,
                new PluginScanner(config),
                new PluginManifestParser()
        );
    }

    private LoaderConfig config(Path directory, boolean strict, String iconName) {
        return new LoaderConfig(
                directory,
                Constants.METADATA_NAME,
                Constants.PLUGIN_EXTENSION,
                iconName,
                Constants.DEFAULT_GAME_PROVIDER_ID,
                Environment.BOTH,
                true,
                strict
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

    private Path createEmptyJar(Path destination) throws IOException {
        Files.createDirectories(destination.getParent());
        try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(destination))) {
            jos.putNextEntry(new JarEntry("META-INF/MANIFEST.MF"));
            jos.write("Manifest-Version: 1.0\n".getBytes(StandardCharsets.UTF_8));
            jos.closeEntry();
        }
        return destination;
    }

    private void createPluginJar(Path destination, String id) throws IOException {
        Files.createDirectories(destination.getParent());
        String yaml = """
                schema: %d
                id: %s
                name: %s plugin
                version: 1.0.0
                environment: both
                """.formatted(SCHEMA, id, id);

        try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(destination))) {
            jos.putNextEntry(new JarEntry(Constants.METADATA_NAME));
            jos.write(yaml.getBytes(StandardCharsets.UTF_8));
            jos.closeEntry();
        }
    }

    private static Object invoke(
            DefaultPluginManager manager,
            String methodName,
            Class<?>[] parameterTypes,
            Object... arguments
    ) throws Exception {
        Method method = DefaultPluginManager.class.getDeclaredMethod(methodName, parameterTypes);
        method.setAccessible(true);
        return method.invoke(manager, arguments);
    }

    @SuppressWarnings("unchecked")
    private static Set<String> preparedPluginIds(DefaultPluginManager manager) throws Exception {
        Field field = DefaultPluginManager.class.getDeclaredField("preparedPlugins");
        field.setAccessible(true);
        return (Set<String>) field.get(manager);
    }

    @SuppressWarnings("unchecked")
    private static List<String> loadedPluginOrder(DefaultPluginManager manager) throws Exception {
        Field field = DefaultPluginManager.class.getDeclaredField("loadedPluginOrder");
        field.setAccessible(true);
        return (List<String>) field.get(manager);
    }
}
