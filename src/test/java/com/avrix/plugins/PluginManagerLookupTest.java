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
import org.junit.jupiter.api.Nested;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests lookup, version, and load-result queries exposed by {@link PluginManager}.
 */
@DisplayName("PluginManager lookup API tests")
class PluginManagerLookupTest {

    @TempDir
    Path tempRoot;

    @Nested
    @DisplayName("Default method contracts")
    class DefaultMethodTests {

        @Test
        @DisplayName("Should find loaded plugins and filter out core components")
        void shouldFindPluginAndFilterCoreComponents() throws IOException {
            Path userJar = Files.writeString(tempRoot.resolve("user.jar"), "jar");
            PluginManager manager = stubManager(
                    new PluginData(metadata("core-component", "1.0.0")),
                    new PluginData(userJar, null, null, metadata("user-one", "1.2.3"))
            );

            assertThat(manager.findPlugin("core-component")).isPresent();
            assertThat(manager.findPlugin("user-one")).isPresent();
            assertThat(manager.findPlugin("missing")).isEmpty();
            assertThat(manager.getUserPlugins())
                    .extracting(PluginData::id)
                    .containsExactly("user-one");
        }

        @Test
        @DisplayName("Should return the last recorded load result for an identifier")
        void shouldReturnLastLoadResult() {
            PluginLoadResult first = PluginLoadResult.loaded("attempted");
            PluginLoadResult second = PluginLoadResult.failed("attempted", new IllegalStateException("boom"));
            PluginLoadResult last = PluginLoadResult.skipped("attempted", "retried later");
            PluginManager manager = new StubPluginManager(
                    Map.of(),
                    List.of(first, second, last, PluginLoadResult.loaded("other"))
            );

            assertThat(manager.findLoadResult("attempted")).contains(last);
            assertThat(manager.findLoadResult("missing")).isEmpty();
        }

        @Test
        @DisplayName("Should evaluate loaded state against semantic version constraints")
        void shouldEvaluateVersionConstraints() throws IOException {
            Path userJar = Files.writeString(tempRoot.resolve("user.jar"), "jar");
            PluginManager manager = stubManager(
                    new PluginData(userJar, null, null, metadata("user-one", "1.2.3"))
            );

            assertThat(manager.isLoaded("user-one")).isTrue();
            assertThat(manager.isLoaded("user-one", "1.2.3")).isTrue();
            assertThat(manager.isLoaded("user-one", ">=1.0.0")).isTrue();
            assertThat(manager.isLoaded("user-one", "^1.2.0")).isTrue();
            assertThat(manager.isLoaded("user-one", "^2.0.0")).isFalse();
            assertThat(manager.isLoaded("missing")).isFalse();
            assertThat(manager.isLoaded("missing", ">=1.0.0")).isFalse();
        }

        @Test
        @DisplayName("Should reject null, blank, and malformed lookup arguments")
        void shouldRejectInvalidLookupArguments() throws IOException {
            Path userJar = Files.writeString(tempRoot.resolve("user.jar"), "jar");
            PluginManager manager = stubManager(
                    new PluginData(userJar, null, null, metadata("user-one", "1.0.0"))
            );

            assertThatThrownBy(() -> manager.findPlugin(null)).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> manager.findPlugin("  ")).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> manager.findLoadResult(null)).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> manager.findLoadResult("  ")).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> manager.isLoaded(null)).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> manager.isLoaded("  ")).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> manager.isLoaded("user-one", null))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> manager.isLoaded("user-one", " "))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> manager.isLoaded("user-one", "not a constraint"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Should report no classpath conflicts by default")
        void shouldReportNoConflictsByDefault() {
            PluginManager manager = new StubPluginManager(Map.of(), List.of());

            assertThat(manager.getClasspathConflicts()).isEmpty();
            assertThatThrownBy(() -> manager.getClasspathConflicts().add(
                    new ClasspathConflict("a/A.class", List.of("a"), "a")
            )).isInstanceOf(UnsupportedOperationException.class);
        }

        private PluginManager stubManager(PluginData... plugins) {
            Map<String, PluginData> registry = new LinkedHashMap<>();
            for (PluginData plugin : plugins) {
                registry.put(plugin.id(), plugin);
            }
            return new StubPluginManager(registry, List.of());
        }
    }

    @Nested
    @DisplayName("DefaultPluginManager integration")
    class IntegrationTests {

        private Path pluginsDirectory;
        private KnotClassLoader classLoader;
        private DefaultPluginManager manager;

        @BeforeEach
        void setUp() throws IOException {
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
            }
        }

        @Test
        @DisplayName("Should expose loaded, failed, and skipped plugins through lookup methods")
        void shouldExposeLoadOutcomes() {
            createPluginJar(pluginsDirectory.resolve("alpha.jar"), metadata("alpha", "1.2.0"));
            createPluginJar(pluginsDirectory.resolve("beta.jar"), metadata("beta", "2.0.0"));
            createPluginJar(
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
            createPluginJar(pluginsDirectory.resolve("skipped.jar"), metadata("skipped", "1.0.0"));
            writeDisabledPlugins("skipped");

            manager.loadPlugins();

            assertThat(manager.findPlugin("alpha")).isPresent();
            assertThat(manager.findPlugin(TestGameProvider.ID)).isPresent();
            assertThat(manager.findPlugin("failing")).isEmpty();
            assertThat(manager.getUserPlugins())
                    .extracting(PluginData::id)
                    .containsExactly("alpha", "beta");
            assertThat(manager.findLoadResult("alpha"))
                    .get()
                    .extracting(PluginLoadResult::status)
                    .isEqualTo(PluginLoadStatus.LOADED);
            assertThat(manager.findLoadResult("failing"))
                    .get()
                    .extracting(PluginLoadResult::status)
                    .isEqualTo(PluginLoadStatus.FAILED);
            assertThat(manager.findLoadResult("skipped"))
                    .get()
                    .extracting(PluginLoadResult::status)
                    .isEqualTo(PluginLoadStatus.SKIPPED);
            assertThat(manager.isLoaded("failing")).isFalse();
            assertThat(manager.isLoaded("failing", ">=0.0.1")).isFalse();
            assertThat(manager.isLoaded("skipped")).isFalse();
            assertThat(manager.isLoaded("skipped", ">=0.0.1")).isFalse();
            assertThat(manager.isLoaded("alpha", "^1.0.0")).isTrue();
            assertThat(manager.isLoaded("alpha", ">=1.0.0")).isTrue();
            assertThat(manager.isLoaded("alpha", "2.0.0")).isFalse();
            assertThat(manager.isLoaded("missing", ">=1.0.0")).isFalse();
        }

        private void writeDisabledPlugins(String id) {
            try {
                Files.writeString(
                        pluginsDirectory.resolve(Constants.DISABLED_PLUGINS_FILE_NAME),
                        id + System.lineSeparator()
                );
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to write disabled plugin list", e);
            }
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

    private static void createPluginJar(Path destination, Metadata metadata, Class<?>... classes) {
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

                StringBuilder yaml = new StringBuilder("""
                        schema: %d
                        id: %s
                        name: %s
                        version: %s
                        environment: both
                        """.formatted(metadata.schema(), metadata.id(), metadata.name(), metadata.version()));
                List<String> entrypoints = metadata.entrypointsFor(Environment.BOTH);
                if (!entrypoints.isEmpty()) {
                    yaml.append("entrypoint: ").append(entrypoints.getFirst()).append('\n');
                }

                jar.putNextEntry(new JarEntry(Constants.METADATA_NAME));
                jar.write(yaml.toString().getBytes(StandardCharsets.UTF_8));
                jar.closeEntry();
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to create plugin archive: " + destination, e);
        }
    }

    /**
     * Minimal plugin manager exposing fixed lookup state.
     */
    private static final class StubPluginManager implements PluginManager {

        private final Map<String, PluginData> plugins;
        private final List<PluginLoadResult> results;

        private StubPluginManager(Map<String, PluginData> plugins, List<PluginLoadResult> results) {
            this.plugins = Map.copyOf(plugins);
            this.results = List.copyOf(results);
        }

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
            return plugins;
        }

        /** {@inheritDoc} */
        @Override
        public List<PluginLoadResult> getLoadResults() {
            return results;
        }

        /** {@inheritDoc} */
        @Override
        public List<Metadata> resolvePluginLoadOrder(List<Metadata> candidates) {
            return candidates;
        }
    }
}
