package com.avrix.plugins;

import com.avrix.core.Environment;
import com.avrix.core.KnotClassLoader;
import com.avrix.core.LoaderConfig;
import com.avrix.core.Metadata;
import com.avrix.provider.DefaultLoaderProvider;
import com.avrix.utils.Constants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import test.loader.TestGameProvider;
import test.plugin.ErrorThrowingPlugin;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests per-plugin data directory wiring in {@link DefaultPluginManager}.
 */
@DisplayName("DefaultPluginManager data directory tests")
class DefaultPluginManagerDataDirectoryTest {

    private static final List<PluginData> CAPTURED = new CopyOnWriteArrayList<>();

    @TempDir
    Path tempRoot;

    private Path pluginsDirectory;
    private KnotClassLoader classLoader;
    private DefaultPluginManager manager;

    @BeforeEach
    void setUp() throws IOException {
        CAPTURED.clear();
        pluginsDirectory = tempRoot.resolve("plugins");
        classLoader = new KnotClassLoader(new URL[0], getClass().getClassLoader());

        LoaderConfig config = LoaderConfig.defaults()
                .withPluginsDirectory(pluginsDirectory)
                .withEnvironment(Environment.BOTH);

        manager = new DefaultPluginManager(
                config,
                classLoader,
                new TestGameProvider(),
                new DefaultLoaderProvider(),
                mixin -> {
                },
                new PluginScanner(config),
                new PluginManifestParser()
        );
        manager.init();
    }

    @AfterEach
    void tearDown() throws IOException {
        try {
            if (manager != null) {
                manager.close();
            }
        } finally {
            if (classLoader != null) {
                classLoader.close();
            }
            CAPTURED.clear();
        }
    }

    @Test
    @DisplayName("Should use plugins/{id} and create it on first access")
    void shouldCreateDirectoryOnFirstAccess() throws IOException {
        createPluginJar(pluginsDirectory.resolve("alpha.jar"), metadata("alpha", "1.0.0"));

        manager.loadPlugins();

        PluginData loaded = manager.getPlugins().get("alpha");
        Path expected = pluginsDirectory.resolve("alpha").toAbsolutePath().normalize();

        assertThat(loaded).isNotNull();
        assertThat(expected).doesNotExist();
        assertThat(loaded.getDataDirectory()).contains(expected);
        assertThat(expected).isDirectory();
        Files.writeString(expected.resolve("state.txt"), "ok");
        assertThat(expected.resolve("state.txt")).hasContent("ok");
    }

    @Test
    @DisplayName("Should create plugins/{id} for a library-only plugin")
    void shouldCreateDirectoryForLibraryOnlyPlugin() {
        Metadata library = new Metadata.Builder()
                .schema(Constants.METADATA_SCHEMA)
                .id("library-only")
                .name("Library only")
                .version("1.0.0")
                .environment(Environment.BOTH)
                .build();
        createPluginJar(pluginsDirectory.resolve("library.jar"), library);

        manager.loadPlugins();

        PluginData loaded = manager.getPlugins().get("library-only");
        Path expected = pluginsDirectory.resolve("library-only").toAbsolutePath().normalize();

        assertThat(loaded).isNotNull();
        assertThat(loaded.getPluginInstance()).isEmpty();
        assertThat(loaded.instances()).isEmpty();
        assertThat(expected).doesNotExist();
        assertThat(loaded.getDataDirectory()).contains(expected);
        assertThat(expected).isDirectory();
    }

    @Test
    @DisplayName("Should expose distinct data directories for different plugins")
    void shouldUseDistinctDirectories() {        createPluginJar(pluginsDirectory.resolve("alpha.jar"), metadata("alpha", "1.0.0"));
        createPluginJar(pluginsDirectory.resolve("beta.jar"), metadata("beta", "1.0.0"));

        manager.loadPlugins();

        Path alphaDirectory = manager.getPlugins().get("alpha").getDataDirectory().orElseThrow();
        Path betaDirectory = manager.getPlugins().get("beta").getDataDirectory().orElseThrow();

        assertThat(alphaDirectory).isNotEqualTo(betaDirectory);
        assertThat(alphaDirectory.getFileName().toString()).isEqualTo("alpha");
        assertThat(betaDirectory.getFileName().toString()).isEqualTo("beta");
    }

    @Test
    @DisplayName("Should give every entrypoint of one plugin the same data directory")
    void shouldShareDirectoryBetweenEntrypoints() {
        Metadata metadata = new Metadata.Builder()
                .schema(Constants.METADATA_SCHEMA_2)
                .id("multi")
                .name("Multi entrypoint plugin")
                .version("1.0.0")
                .environment(Environment.BOTH)
                .mainEntrypoints(List.of(FirstEntrypoint.class.getName(), SecondEntrypoint.class.getName()))
                .build();
        Path jar = createPluginJar(
                pluginsDirectory.resolve("multi.jar"),
                metadata,
                FirstEntrypoint.class,
                SecondEntrypoint.class
        );

        manager.loadPlugin(new PluginData(jar, null, null, metadata));

        Path expected = pluginsDirectory.resolve("multi").toAbsolutePath().normalize();
        assertThat(CAPTURED).hasSize(2);
        assertThat(CAPTURED.get(0).getDataDirectory()).contains(expected);
        assertThat(CAPTURED.get(1).getDataDirectory()).contains(expected);
        assertThat(manager.getPlugins().get("multi").getDataDirectory()).contains(expected);
    }

    @Test
    @DisplayName("Should not create a directory for a plugin that fails to start")
    void shouldNotCreateDirectoryForFailedPlugin() {
        Path failingJar = createPluginJar(
                pluginsDirectory.resolve("failing.jar"),
                new Metadata.Builder()
                        .schema(Constants.METADATA_SCHEMA)
                        .id("failing")
                        .name("Failing")
                        .version("1.0.0")
                        .environment(Environment.BOTH)
                        .entrypoint(ErrorThrowingPlugin.class.getName())
                        .build(),
                ErrorThrowingPlugin.class
        );

        manager.loadPlugins();

        assertThat(manager.getPlugins()).doesNotContainKey("failing");
        assertThat(pluginsDirectory.resolve("failing")).doesNotExist();
        assertThat(failingJar).exists();
    }

    @Test
    @DisplayName("Should return an empty data directory for core components")
    void shouldReturnEmptyForCorePlugins() {
        manager.loadPlugins();

        assertThat(manager.getPlugins().get(TestGameProvider.ID).getDataDirectory()).isEmpty();
        assertThat(manager.getPlugins().get(Constants.LOADER_ID).getDataDirectory()).isEmpty();
    }

    @Test
    @DisplayName("Should fail with UncheckedIOException when a file occupies the plugin directory path")
    void shouldFailWhenFileOccupiesPluginDirectory() throws IOException {
        Files.writeString(pluginsDirectory.resolve("blocked"), "not a directory");
        createPluginJar(pluginsDirectory.resolve("blocked.jar"), metadata("blocked", "1.0.0"));

        manager.loadPlugins();

        PluginData loaded = manager.getPlugins().get("blocked");
        assertThat(loaded).isNotNull();
        assertThatThrownBy(loaded::getDataDirectory).isInstanceOf(UncheckedIOException.class);
    }

    /**
     * First entrypoint probe recording the received descriptor.
     */
    public static final class FirstEntrypoint implements Plugin {

        /** {@inheritDoc} */
        @Override
        public void onInitialize(PluginData pluginData) {
            CAPTURED.add(pluginData);
        }
    }

    /**
     * Second entrypoint probe recording the received descriptor.
     */
    public static final class SecondEntrypoint implements Plugin {

        /** {@inheritDoc} */
        @Override
        public void onInitialize(PluginData pluginData) {
            CAPTURED.add(pluginData);
        }
    }

    private static Metadata metadata(String id, String version) {
        return new Metadata.Builder()
                .schema(Constants.METADATA_SCHEMA)
                .id(id)
                .name(id)
                .version(version)
                .environment(Environment.BOTH)
                .build();
    }

    private static Path createPluginJar(Path destination, Metadata metadata, Class<?>... classes) {
        try {
            Files.createDirectories(destination.getParent());
            try (JarOutputStream jar = new JarOutputStream(Files.newOutputStream(destination))) {
                for (Class<?> type : classes) {
                    String classPath = type.getName().replace('.', '/') + ".class";
                    try (var bytes = type.getClassLoader().getResourceAsStream(classPath)) {
                        if (bytes == null) {
                            throw new IllegalStateException("Class resource not found: " + classPath);
                        }
                        jar.putNextEntry(new JarEntry(classPath));
                        jar.write(bytes.readAllBytes());
                        jar.closeEntry();
                    }
                }

                jar.putNextEntry(new JarEntry(Constants.METADATA_NAME));
                jar.write(metadataYaml(metadata).getBytes(StandardCharsets.UTF_8));
                jar.closeEntry();
            }
            return destination;
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to create plugin archive: " + destination, e);
        }
    }

    private static String metadataYaml(Metadata metadata) {
        StringBuilder yaml = new StringBuilder("""
                schema: %d
                id: %s
                name: %s
                version: %s
                environment: both
                """.formatted(metadata.schema(), metadata.id(), metadata.name(), metadata.version()));

        List<String> entrypoints = metadata.entrypointsFor(Environment.BOTH);
        if (metadata.schema() >= Constants.METADATA_SCHEMA_2) {
            if (!entrypoints.isEmpty()) {
                yaml.append("entrypoints:\n  main:\n");
                for (String entrypoint : entrypoints) {
                    yaml.append("    - \"").append(entrypoint).append("\"\n");
                }
            }
        } else if (!entrypoints.isEmpty()) {
            yaml.append("entrypoint: ").append(entrypoints.getFirst()).append('\n');
        }
        return yaml.toString();
    }
}
