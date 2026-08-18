package com.avrix.plugins;

import com.avrix.core.Environment;
import com.avrix.core.KnotClassLoader;
import com.avrix.core.Metadata;
import com.avrix.core.ServiceManager;
import com.avrix.provider.DefaultLoaderProvider;
import com.avrix.provider.GameProvider;
import com.avrix.provider.LoaderProvider;
import com.avrix.utils.Constants;
import org.junit.jupiter.api.*;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit test suite for {@link DefaultPluginManager} dependency graph resolution and lifecycle operations.
 */
@DisplayName("DefaultPluginManager Unit Tests")
class DefaultPluginManagerTest {

    private DefaultPluginManager manager;
    private static final int SCHEMA = Constants.METADATA_SCHEMA;

    @BeforeEach
    void setUp() {
        ServiceManager.clear();

        KnotClassLoader classLoader = new KnotClassLoader(new URL[0], ClassLoader.getSystemClassLoader());
        Thread.currentThread().setContextClassLoader(classLoader);
        ServiceManager.register(KnotClassLoader.class, classLoader);

        GameProvider dummyProvider = new DummyGameProvider();
        ServiceManager.register(GameProvider.class, dummyProvider);

        LoaderProvider loaderProvider = new DefaultLoaderProvider();
        ServiceManager.register(LoaderProvider.class, loaderProvider);

        manager = new DefaultPluginManager();
    }


    @AfterEach
    void tearDown() {
        ServiceManager.clear();
    }

    private Metadata createMeta(String id, String version) {
        return new Metadata.Builder()
                .schema(SCHEMA)
                .id(id)
                .name(id + "-plugin")
                .version(version)
                .environment(Environment.BOTH)
                .build();
    }

    private Metadata createMetaWithDeps(String id, String version, Map<String, String> deps) {
        return new Metadata.Builder()
                .schema(SCHEMA)
                .id(id)
                .name(id + "-plugin")
                .version(version)
                .environment(Environment.BOTH)
                .dependencies(deps)
                .build();
    }

    @Nested
    @DisplayName("Dependency Resolution & Topological Sorting")
    class ResolutionTests {

        @Test
        @DisplayName("Should sort independent plugins without ordering constraints")
        void shouldSortIndependentPlugins() {
            var meta1 = createMeta("pluginA", "1.0.0");
            var meta2 = createMeta("pluginB", "1.0.0");
            var meta3 = createMeta("pluginC", "1.0.0");

            var result = manager.resolvePluginLoadOrder(List.of(meta1, meta2, meta3));

            assertThat(result)
                    .hasSize(3)
                    .containsExactlyInAnyOrder(meta1, meta2, meta3);
        }

        @Test
        @DisplayName("Should sort linearly dependent plugins in prerequisite order")
        void shouldSortLinearDependencies() {
            // A -> B -> C (A depends on B, B depends on C)
            // Expected load order: C, B, A
            var metaA = createMetaWithDeps("pluginA", "1.0.0", Map.of("pluginB", ">=1.0.0"));
            var metaB = createMetaWithDeps("pluginB", "1.0.0", Map.of("pluginC", ">=1.0.0"));
            var metaC = createMeta("pluginC", "1.0.0");

            var result = manager.resolvePluginLoadOrder(List.of(metaA, metaC, metaB));

            assertThat(result)
                    .extracting(Metadata::id)
                    .containsExactly("pluginC", "pluginB", "pluginA");
        }

        @Test
        @DisplayName("Should resolve and sort complex Directed Acyclic Graph (DAG)")
        void shouldSortComplexGraph() {
            // A depends on B and C; B and C depend on D
            // Expected load order: D, (B/C in any order), A
            var metaA = createMetaWithDeps("A", "1.0.0", Map.of("B", ">=1.0.0", "C", ">=1.0.0"));
            var metaB = createMetaWithDeps("B", "1.0.0", Map.of("D", ">=1.0.0"));
            var metaC = createMetaWithDeps("C", "1.0.0", Map.of("D", ">=1.0.0"));
            var metaD = createMeta("D", "1.0.0");

            var result = manager.resolvePluginLoadOrder(List.of(metaA, metaB, metaC, metaD));

            assertThat(result).hasSize(4);
            assertThat(result.get(0).id()).isEqualTo("D");
            assertThat(result.get(3).id()).isEqualTo("A");
            assertThat(result.subList(1, 3)).extracting(Metadata::id).containsExactlyInAnyOrder("B", "C");
        }

        @Test
        @DisplayName("Should throw IllegalStateException when a required dependency is missing")
        void shouldFailOnMissingDependency() {
            var metaA = createMetaWithDeps("pluginA", "1.0.0", Map.of("missing-plugin", ">=1.0.0"));

            assertThatThrownBy(() -> manager.resolvePluginLoadOrder(List.of(metaA)))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Plugin [pluginA] depends on missing component [missing-plugin]");
        }

        @Test
        @DisplayName("Should detect and reject cyclic plugin dependency graphs")
        void shouldFailOnCircularDependency() {
            // A -> B -> C -> A
            var metaA = createMetaWithDeps("A", "1.0.0", Map.of("B", ">=1.0.0"));
            var metaB = createMetaWithDeps("B", "1.0.0", Map.of("C", ">=1.0.0"));
            var metaC = createMetaWithDeps("C", "1.0.0", Map.of("A", ">=1.0.0"));

            assertThatThrownBy(() -> manager.resolvePluginLoadOrder(List.of(metaA, metaB, metaC)))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cyclic plugin dependency detected")
                    .hasMessageContaining("A")
                    .hasMessageContaining("B")
                    .hasMessageContaining("C");
        }
    }

    @Nested
    @DisplayName("SemVer Constraint Verification")
    class SemVerTests {

        @Test
        @DisplayName("Should accept valid SemVer caret constraints")
        void shouldAcceptValidConstraints() {
            var metaA = createMetaWithDeps("pluginA", "1.0.0", Map.of("pluginB", "^2.1.0"));
            var metaB = createMeta("pluginB", "2.1.5"); // 2.1.5 satisfies ^2.1.0

            var result = manager.resolvePluginLoadOrder(List.of(metaA, metaB));

            assertThat(result).extracting(Metadata::id).containsExactly("pluginB", "pluginA");
        }

        @Test
        @DisplayName("Should reject incompatible SemVer versions")
        void shouldRejectInvalidConstraints() {
            var metaA = createMetaWithDeps("pluginA", "1.0.0", Map.of("pluginB", "^2.1.0"));
            var metaB = createMeta("pluginB", "3.0.0"); // 3.0.0 violates ^2.1.0

            assertThatThrownBy(() -> manager.resolvePluginLoadOrder(List.of(metaA, metaB)))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Plugin [pluginA] requires [pluginB] matching [^2.1.0], but found version [3.0.0]");
        }

        @Test
        @DisplayName("Should throw IllegalStateException when version format is malformed")
        void shouldFailOnMalformedDependencyVersion() {
            var metaA = createMetaWithDeps("pluginA", "1.0.0", Map.of("pluginB", ">=1.0.0"));
            var metaB = createMeta("pluginB", "invalid-semver-string");

            assertThatThrownBy(() -> manager.resolvePluginLoadOrder(List.of(metaA, metaB)))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Invalid version constraint in plugin [pluginA] for dependency [pluginB]");
        }
    }

    @Nested
    @DisplayName("Manager Lifecycle Tests")
    class LifecycleTests {

        @Test
        @DisplayName("Should initialize manager and register core platform descriptors")
        void shouldInitializeSuccessfully() {
            manager.init();

            var allPlugins = manager.getPlugins();

            assertThat(allPlugins).containsKey(Constants.LOADER_ID);
            assertThat(allPlugins).containsKey("dummy-game");
        }

        @Test
        @DisplayName("Should prevent double initialization")
        void shouldPreventDoubleInitialization() {
            manager.init();

            assertThatThrownBy(manager::init)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("PluginManager is already initialized");
        }
    }

    /**
     * Stub {@link GameProvider} for plugin manager unit tests.
     */
    private static final class DummyGameProvider implements GameProvider {
        @Override
        public String getId() {
            return "dummy-game";
        }

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
            return "Dummy Game";
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
            return Paths.get(".").toAbsolutePath().normalize();
        }

        @Override
        public Environment getEnvironment() {
            return Environment.BOTH;
        }

        @Override
        public String getEntrypoint() {
            return "com.dummy.Main";
        }

        @Override
        public List<String> getAuthors() {
            return List.of("Test Author");
        }

        @Override
        public String getLicense() {
            return "MIT";
        }

        @Override
        public List<String> getContacts() {
            return List.of();
        }
    }
}