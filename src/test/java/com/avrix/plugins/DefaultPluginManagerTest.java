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
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit test suite for {@link DefaultPluginManager} dependency graph resolution,
 * soft load ordering rules, and lifecycle operations.
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
                    .hasMessageContaining("Cyclic plugin dependency or load order detected")
                    .hasMessageContaining("A")
                    .hasMessageContaining("B")
                    .hasMessageContaining("C");
        }
    }

    @Nested
    @DisplayName("Soft Load Ordering (loadBefore & loadAfter) Tests")
    class SoftOrderingTests {

        @Test
        @DisplayName("Should respect direct loadAfter constraints without breaking missing soft targets")
        void shouldRespectLoadAfter() {
            // Plugin A specifies loadAfter: [B]
            // Expected load order: B, A
            var metaA = new Metadata.Builder()
                    .schema(SCHEMA)
                    .id("pluginA")
                    .name("A")
                    .version("1.0.0")
                    .loadAfter("pluginB", "optional-missing-plugin")
                    .build();
            var metaB = createMeta("pluginB", "1.0.0");

            var result = manager.resolvePluginLoadOrder(List.of(metaA, metaB));

            assertThat(result)
                    .extracting(Metadata::id)
                    .containsExactly("pluginB", "pluginA");
        }

        @Test
        @DisplayName("Should respect direct loadBefore constraints")
        void shouldRespectLoadBefore() {
            // Plugin A specifies loadBefore: [B]
            // Expected load order: A, B
            var metaA = new Metadata.Builder()
                    .schema(SCHEMA)
                    .id("pluginA")
                    .name("A")
                    .version("1.0.0")
                    .loadBefore("pluginB")
                    .build();
            var metaB = createMeta("pluginB", "1.0.0");

            var result = manager.resolvePluginLoadOrder(List.of(metaB, metaA));

            assertThat(result)
                    .extracting(Metadata::id)
                    .containsExactly("pluginA", "pluginB");
        }

        @Test
        @DisplayName("Should position plugin with loadBefore wildcard '*' at the head of the load order")
        void shouldPositionWildcardLoadBeforeAtBeginning() {
            var metaEarly = new Metadata.Builder()
                    .schema(SCHEMA)
                    .id("early-init")
                    .name("Early")
                    .version("1.0.0")
                    .loadBefore("*")
                    .build();
            var meta1 = createMeta("normal1", "1.0.0");
            var meta2 = createMeta("normal2", "1.0.0");
            var meta3 = createMeta("normal3", "1.0.0");

            var result = manager.resolvePluginLoadOrder(List.of(meta2, metaEarly, meta3, meta1));

            assertThat(result.getFirst().id()).isEqualTo("early-init");
        }

        @Test
        @DisplayName("Should position plugin with loadAfter wildcard '*' at the tail of the load order")
        void shouldPositionWildcardLoadAfterAtEnd() {
            var metaLate = new Metadata.Builder()
                    .schema(SCHEMA)
                    .id("late-init")
                    .name("Late")
                    .version("1.0.0")
                    .loadAfter("*")
                    .build();
            var meta1 = createMeta("normal1", "1.0.0");
            var meta2 = createMeta("normal2", "1.0.0");

            var result = manager.resolvePluginLoadOrder(List.of(metaLate, meta1, meta2));

            assertThat(result.getLast().id()).isEqualTo("late-init");
        }

        @Test
        @DisplayName("Should correctly order chain containing both early wildcard and late wildcard plugins")
        void shouldCombineEarlyAndLateWildcards() {
            var early = new Metadata.Builder().schema(SCHEMA).id("early").name("E").version("1.0.0").loadBefore("*").build();
            var late = new Metadata.Builder().schema(SCHEMA).id("late").name("L").version("1.0.0").loadAfter("*").build();
            var mid1 = createMeta("mid1", "1.0.0");
            var mid2 = createMeta("mid2", "1.0.0");

            var result = manager.resolvePluginLoadOrder(List.of(late, mid1, early, mid2));

            assertThat(result.getFirst().id()).isEqualTo("early");
            assertThat(result.getLast().id()).isEqualTo("late");
            assertThat(result.subList(1, 3)).extracting(Metadata::id).containsExactlyInAnyOrder("mid1", "mid2");
        }

        @Test
        @DisplayName("Should detect circular dependency introduced by conflicting loadBefore and loadAfter")
        void shouldFailOnConflictingSoftRules() {
            // A loadBefore B and B loadBefore A
            var metaA = new Metadata.Builder().schema(SCHEMA).id("A").name("A").version("1.0.0").loadBefore("B").build();
            var metaB = new Metadata.Builder().schema(SCHEMA).id("B").name("B").version("1.0.0").loadBefore("A").build();

            assertThatThrownBy(() -> manager.resolvePluginLoadOrder(List.of(metaA, metaB)))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cyclic plugin dependency or load order detected");
        }

        @Test
        @DisplayName("Should resolve complex multi-tiered diamond graph with interleaved soft rules (7 plugins)")
        void shouldResolveComplexDiamondWithInterleavedSoftRules() {
            var coreLib = createMeta("core-lib", "1.0.0");

            var dbService = new Metadata.Builder()
                    .schema(SCHEMA).id("db-service").name("DB").version("1.0.0")
                    .dependencies(Map.of("core-lib", ">=1.0.0"))
                    .build();

            var authService = new Metadata.Builder()
                    .schema(SCHEMA).id("auth-service").name("Auth").version("1.0.0")
                    .dependencies(Map.of("core-lib", ">=1.0.0"))
                    .build();

            var businessLogic = new Metadata.Builder()
                    .schema(SCHEMA).id("biz-logic").name("Biz").version("1.0.0")
                    .dependencies(Map.of("db-service", ">=1.0.0", "auth-service", ">=1.0.0"))
                    .loadBefore("ui-service")
                    .build();

            var metricsService = new Metadata.Builder()
                    .schema(SCHEMA).id("metrics").name("Metrics").version("1.0.0")
                    .loadAfter("db-service")
                    .loadBefore("ui-service")
                    .build();

            var uiService = new Metadata.Builder()
                    .schema(SCHEMA).id("ui-service").name("UI").version("1.0.0")
                    .dependencies(Map.of("core-lib", ">=1.0.0"))
                    .build();

            var reporter = new Metadata.Builder()
                    .schema(SCHEMA).id("reporter").name("Reporter").version("1.0.0")
                    .loadAfter("*")
                    .build();

            var candidates = List.of(reporter, uiService, businessLogic, metricsService, authService, dbService, coreLib);
            var sorted = manager.resolvePluginLoadOrder(candidates);

            assertThat(sorted).hasSize(7);

            List<String> order = sorted.stream().map(Metadata::id).toList();

            assertThat(order.indexOf("core-lib")).isLessThan(order.indexOf("db-service"));
            assertThat(order.indexOf("core-lib")).isLessThan(order.indexOf("auth-service"));
            assertThat(order.indexOf("core-lib")).isLessThan(order.indexOf("ui-service"));
            assertThat(order.indexOf("db-service")).isLessThan(order.indexOf("biz-logic"));
            assertThat(order.indexOf("auth-service")).isLessThan(order.indexOf("biz-logic"));
            assertThat(order.indexOf("db-service")).isLessThan(order.indexOf("metrics"));
            assertThat(order.indexOf("metrics")).isLessThan(order.indexOf("ui-service"));
            assertThat(order.indexOf("biz-logic")).isLessThan(order.indexOf("ui-service"));
            assertThat(order.getLast()).isEqualTo("reporter");
        }

        @Test
        @DisplayName("Should prioritize hard dependency over wildcard loadBefore rule")
        void shouldRespectHardDependencyOverWildcardLoadBefore() {
            var bootFramework = createMeta("boot-framework", "1.0.0");

            var eagerPlugin = new Metadata.Builder()
                    .schema(SCHEMA)
                    .id("eager-plugin")
                    .name("Eager")
                    .version("1.0.0")
                    .dependencies(Map.of("boot-framework", ">=1.0.0"))
                    .loadBefore("*")
                    .build();

            var regularMod1 = createMeta("mod-one", "1.0.0");
            var regularMod2 = createMeta("mod-two", "1.0.0");

            var sorted = manager.resolvePluginLoadOrder(List.of(regularMod2, eagerPlugin, regularMod1, bootFramework));

            List<String> order = sorted.stream().map(Metadata::id).toList();

            assertThat(order.indexOf("boot-framework")).isZero();
            assertThat(order.indexOf("eager-plugin")).isEqualTo(1);
            assertThat(order.indexOf("eager-plugin")).isLessThan(order.indexOf("mod-one"));
            assertThat(order.indexOf("eager-plugin")).isLessThan(order.indexOf("mod-two"));
        }

        @Test
        @DisplayName("Should detect multi-step indirect circular dependency across 5 plugins via soft ordering")
        void shouldDetectIndirectLongCycle() {
            var p1 = new Metadata.Builder().schema(SCHEMA).id("p1").name("P1").version("1.0").loadBefore("p2").build();
            var p2 = new Metadata.Builder().schema(SCHEMA).id("p2").name("P2").version("1.0").loadBefore("p3").build();
            var p3 = new Metadata.Builder().schema(SCHEMA).id("p3").name("P3").version("1.0").loadBefore("p4").build();
            var p4 = new Metadata.Builder().schema(SCHEMA).id("p4").name("P4").version("1.0").loadBefore("p5").build();
            var p5 = new Metadata.Builder().schema(SCHEMA).id("p5").name("P5").version("1.0").loadBefore("p1").build();

            assertThatThrownBy(() -> manager.resolvePluginLoadOrder(List.of(p3, p1, p5, p2, p4)))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cyclic plugin dependency or load order detected")
                    .hasMessageContaining("p1")
                    .hasMessageContaining("p5");
        }

        @Test
        @DisplayName("Should resolve two wildcard plugins when disambiguated by explicit loadAfter rule")
        void shouldDisambiguateMultipleWildcardPlugins() {
            var pluginA = new Metadata.Builder()
                    .schema(SCHEMA).id("plugin-a").name("A").version("1.0.0")
                    .loadBefore("*")
                    .build();

            var pluginB = new Metadata.Builder()
                    .schema(SCHEMA).id("plugin-b").name("B").version("1.0.0")
                    .loadBefore("*")
                    .loadAfter("plugin-a")
                    .build();

            var normal1 = createMeta("normal-1", "1.0.0");
            var normal2 = createMeta("normal-2", "1.0.0");

            var sorted = manager.resolvePluginLoadOrder(List.of(normal2, pluginB, normal1, pluginA));
            List<String> order = sorted.stream().map(Metadata::id).toList();

            assertThat(order.get(0)).isEqualTo("plugin-a");
            assertThat(order.get(1)).isEqualTo("plugin-b");
            assertThat(order.subList(2, 4)).containsExactlyInAnyOrder("normal-1", "normal-2");
        }
    }

    @Nested
    @DisplayName("Edge Cases & Conflict Resolution in Topological Sorting")
    class EdgeCaseSortingTests {

        @Test
        @DisplayName("Should throw NullPointerException when candidates list is null")
        void shouldThrowOnNullCandidatesList() {
            assertThatThrownBy(() -> manager.resolvePluginLoadOrder(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("Candidates list cannot be null");
        }

        @Test
        @DisplayName("Should retain first detected instance when duplicate plugin IDs are encountered")
        void shouldHandleDuplicatePluginIds() {
            var metaFirst = new Metadata.Builder().schema(SCHEMA).id("dup-id").name("First").version("1.0.0").build();
            var metaSecond = new Metadata.Builder().schema(SCHEMA).id("dup-id").name("Second").version("2.0.0").build();
            var metaOther = createMeta("other", "1.0.0");

            var result = manager.resolvePluginLoadOrder(List.of(metaFirst, metaSecond, metaOther));

            assertThat(result).hasSize(2);
            assertThat(result).extracting(Metadata::name).contains("First", "other-plugin");
            assertThat(result).extracting(Metadata::name).doesNotContain("Second");
        }

        @Test
        @DisplayName("Should detect self-referencing dependency cycles")
        void shouldDetectSelfCycle() {
            var selfDep = createMetaWithDeps("self-dep", "1.0.0", Map.of("self-dep", ">=1.0.0"));

            assertThatThrownBy(() -> manager.resolvePluginLoadOrder(List.of(selfDep)))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cyclic plugin dependency or load order detected")
                    .hasMessageContaining("self-dep");
        }

        @Test
        @DisplayName("Should correctly handle dependent of a wildcard loadAfter plugin")
        void shouldRespectDependentOfWildcardLoadAfterPlugin() {
            var baseLogger = new Metadata.Builder()
                    .schema(SCHEMA).id("base-logger").name("Base Logger").version("1.0.0")
                    .loadAfter("*")
                    .build();

            var loggerAddon = new Metadata.Builder()
                    .schema(SCHEMA).id("logger-addon").name("Logger Addon").version("1.0.0")
                    .dependencies(Map.of("base-logger", ">=1.0.0"))
                    .build();

            var generalMod1 = createMeta("general-mod-1", "1.0.0");
            var generalMod2 = createMeta("general-mod-2", "1.0.0");

            var result = manager.resolvePluginLoadOrder(List.of(loggerAddon, generalMod2, baseLogger, generalMod1));
            List<String> order = result.stream().map(Metadata::id).toList();

            assertThat(order.indexOf("general-mod-1")).isLessThan(order.indexOf("base-logger"));
            assertThat(order.indexOf("general-mod-2")).isLessThan(order.indexOf("base-logger"));
            assertThat(order.indexOf("base-logger")).isLessThan(order.indexOf("logger-addon"));
        }
    }

    @Nested
    @DisplayName("Plugin Loading & ClassLoader Integration Tests")
    class PluginLoadingTests {

        @TempDir
        Path tempDir;

        @BeforeEach
        void initManagerForLoading() {
            manager.init();
        }

        @Test
        @DisplayName("Should throw NullPointerException when loading null PluginData container")
        void shouldThrowOnNullContainer() {
            assertThatThrownBy(() -> manager.loadPlugin(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("Plugin container cannot be null");
        }

        @Test
        @DisplayName("Should throw IllegalStateException when loading user plugin without physical JAR file")
        void shouldThrowWhenUserPluginLacksPhysicalFile() {
            var syntheticMeta = createMeta("synthetic-user", "1.0.0");
            var syntheticContainer = new PluginData(syntheticMeta);

            assertThatThrownBy(() -> manager.loadPlugin(syntheticContainer))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot load user plugin 'synthetic-user' without a physical JAR file");
        }

        @Test
        @DisplayName("Should load library-only plugin without entrypoint and cache its state")
        void shouldLoadLibraryPlugin() throws Exception {
            var jarFile = createDummyJar("lib-plugin.jar", null);
            var meta = new Metadata.Builder()
                    .schema(SCHEMA)
                    .id("lib-plugin")
                    .name("Lib Plugin")
                    .version("1.0.0")
                    .build();

            var container = new PluginData(jarFile, null, null, meta);
            manager.loadPlugin(container);

            var loaded = manager.getPlugins().get("lib-plugin");
            assertThat(loaded).isNotNull();
            assertThat(loaded.isSynthetic()).isFalse();
            assertThat(loaded.getPluginFile()).contains(jarFile);
        }

        @Test
        @DisplayName("Should instantiate and execute valid Plugin entrypoint lifecycle callback")
        void shouldInstantiateAndRunEntrypoint() throws Exception {
            var jarFile = createDummyJar("executable-plugin.jar", ValidStubPlugin.class);
            var meta = new Metadata.Builder()
                    .schema(SCHEMA)
                    .id("exec-plugin")
                    .name("Exec Plugin")
                    .version("1.0.0")
                    .entrypoint(ValidStubPlugin.class.getName())
                    .build();

            ValidStubPlugin.initializedData = null;

            var container = new PluginData(jarFile, null, null, meta);
            manager.loadPlugin(container);

            var loaded = manager.getPlugins().get("exec-plugin");
            assertThat(loaded).isNotNull();
            assertThat(loaded.isSynthetic()).isFalse();
            assertThat(ValidStubPlugin.initializedData).isNotNull();
            assertThat(ValidStubPlugin.initializedData.id()).isEqualTo("exec-plugin");
        }

        @Test
        @DisplayName("Should throw RuntimeException when declared entrypoint does not implement Plugin interface")
        void shouldFailWhenEntrypointDoesNotImplementPlugin() throws Exception {
            var jarFile = createDummyJar("invalid-plugin.jar", NonPluginClass.class);
            var meta = new Metadata.Builder()
                    .schema(SCHEMA)
                    .id("invalid-plugin")
                    .name("Invalid Plugin")
                    .version("1.0.0")
                    .entrypoint(NonPluginClass.class.getName())
                    .build();

            var container = new PluginData(jarFile, null, null, meta);

            assertThatThrownBy(() -> manager.loadPlugin(container))
                    .isInstanceOf(RuntimeException.class)
                    .hasCauseInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Failed to load plugin: invalid-plugin");
        }

        @Test
        @DisplayName("Should ignore duplicate loadPlugin invocation for already loaded plugin ID")
        void shouldIgnoreDuplicateLoadCalls() throws Exception {
            var jarFile = createDummyJar("dup-run.jar", null);
            var meta = createMeta("dup-run", "1.0.0");
            var container = new PluginData(jarFile, null, null, meta);

            manager.loadPlugin(container);
            // Second call should silently skip
            manager.loadPlugin(container);

            assertThat(manager.getPlugins()).containsKey("dup-run");
        }

        @Test
        @DisplayName("Should return unmodifiable map from getPlugins()")
        void shouldReturnUnmodifiablePluginsMap() {
            var pluginsMap = manager.getPlugins();

            assertThatThrownBy(() -> pluginsMap.put("illegal", new PluginData(createMeta("illegal", "1.0"))))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        private File createDummyJar(String filename, Class<?> classToInclude) throws Exception {
            var jarPath = tempDir.resolve(filename);
            try (var jos = new JarOutputStream(Files.newOutputStream(jarPath))) {
                if (classToInclude != null) {
                    String classPath = classToInclude.getName().replace('.', '/') + ".class";
                    var classBytes = classToInclude.getClassLoader().getResourceAsStream(classPath);
                    if (classBytes != null) {
                        jos.putNextEntry(new JarEntry(classPath));
                        jos.write(classBytes.readAllBytes());
                        jos.closeEntry();
                    }
                }
                jos.putNextEntry(new JarEntry("META-INF/MANIFEST.MF"));
                jos.write("Manifest-Version: 1.0\n".getBytes(StandardCharsets.UTF_8));
                jos.closeEntry();
            }
            return jarPath.toFile();
        }
    }

    @Nested
    @DisplayName("loadPlugins() End-to-End & Disk Discovery Tests")
    class LoadPluginsEndToEndTests {

        @TempDir
        Path customPluginsDir;

        @Test
        @DisplayName("Should throw IllegalStateException when loadPlugins() is called before init()")
        void shouldThrowWhenNotInitialized() {
            var uninitializedManager = new DefaultPluginManager();
            assertThatThrownBy(uninitializedManager::loadPlugins)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("PluginManager is not initialized. Call init() first.");
        }

        @Test
        @DisplayName("Should gracefully handle empty plugins directory without errors")
        void shouldHandleEmptyPluginsFolder() {
            manager.init();
            manager.loadPlugins();

            // Only core plugins (game & loader) should be present
            assertThat(manager.getPlugins())
                    .containsKeys("dummy-game", Constants.LOADER_ID)
                    .hasSize(2);
        }

        @Test
        @DisplayName("Should filter out plugins incompatible with active runtime environment")
        void shouldFilterPluginsByEnvironment() throws Exception {
            ServiceManager.clear();

            KnotClassLoader classLoader = new KnotClassLoader(new URL[0], ClassLoader.getSystemClassLoader());
            Thread.currentThread().setContextClassLoader(classLoader);
            ServiceManager.register(KnotClassLoader.class, classLoader);

            LoaderProvider loaderProvider = new DefaultLoaderProvider();
            ServiceManager.register(LoaderProvider.class, loaderProvider);
            
            // Setup a game provider running specifically in CLIENT environment
            ServiceManager.register(GameProvider.class, new DummyGameProvider() {
                @Override
                public Environment getEnvironment() {
                    return Environment.CLIENT;
                }
            });

            var envManager = new DefaultPluginManager();
            envManager.init();

            Path pluginsPath = Path.of(Constants.PLUGINS_FOLDER_NAME);
            Files.createDirectories(pluginsPath);

            // Server-only plugin JAR (must be skipped)
            var serverMeta = new Metadata.Builder()
                    .schema(SCHEMA).id("server-mod").name("Server Mod").version("1.0.0")
                    .environment(Environment.SERVER)
                    .build();
            createPluginJarOnDisk(pluginsPath.resolve("server-mod.jar"), serverMeta);

            // Client plugin JAR (must be loaded)
            var clientMeta = new Metadata.Builder()
                    .schema(SCHEMA).id("client-mod").name("Client Mod").version("1.0.0")
                    .environment(Environment.CLIENT)
                    .build();
            createPluginJarOnDisk(pluginsPath.resolve("client-mod.jar"), clientMeta);

            try {
                envManager.loadPlugins();

                assertThat(envManager.getPlugins()).containsKey("client-mod");
                assertThat(envManager.getPlugins()).doesNotContainKey("server-mod");
            } finally {
                // Cleanup created files in plugins folder
                Files.deleteIfExists(pluginsPath.resolve("server-mod.jar"));
                Files.deleteIfExists(pluginsPath.resolve("client-mod.jar"));
            }
        }

        @Test
        @DisplayName("Should skip corrupt JAR archives and continue loading valid plugins")
        void shouldSkipCorruptedJarsAndContinue() throws Exception {
            manager.init();
            Path pluginsPath = Path.of(Constants.PLUGINS_FOLDER_NAME);
            Files.createDirectories(pluginsPath);

            // Corrupt file with plugin extension
            Path corruptJar = pluginsPath.resolve("corrupt-plugin.jar");
            Files.writeString(corruptJar, "CORRUPTED_NON_ZIP_DATA");

            // Valid plugin
            var validMeta = createMeta("valid-survivor", "1.0.0");
            Path validJar = pluginsPath.resolve("valid-survivor.jar");
            createPluginJarOnDisk(validJar, validMeta);

            try {
                manager.loadPlugins();

                assertThat(manager.getPlugins()).containsKey("valid-survivor");
            } finally {
                Files.deleteIfExists(corruptJar);
                Files.deleteIfExists(validJar);
            }
        }

        private void createPluginJarOnDisk(Path destination, Metadata metadata) throws Exception {
            try (var jos = new JarOutputStream(Files.newOutputStream(destination))) {
                var entry = new JarEntry(Constants.METADATA_NAME);
                jos.putNextEntry(entry);
                String yaml = """
                        schema: %d
                        id: %s
                        name: %s
                        version: %s
                        environment: %s
                        """.formatted(
                        metadata.schema(),
                        metadata.id(),
                        metadata.name(),
                        metadata.version(),
                        metadata.environment().name().toLowerCase()
                );
                jos.write(yaml.getBytes(StandardCharsets.UTF_8));
                jos.closeEntry();
            }
        }
    }

    @Nested
    @DisplayName("SemVer Constraint Verification")
    class SemVerTests {

        @Test
        @DisplayName("Should accept valid SemVer caret constraints")
        void shouldAcceptValidConstraints() {
            var metaA = createMetaWithDeps("pluginA", "1.0.0", Map.of("pluginB", "^2.1.0"));
            var metaB = createMeta("pluginB", "2.1.5");

            var result = manager.resolvePluginLoadOrder(List.of(metaA, metaB));

            assertThat(result).extracting(Metadata::id).containsExactly("pluginB", "pluginA");
        }

        @Test
        @DisplayName("Should reject incompatible SemVer versions")
        void shouldRejectInvalidConstraints() {
            var metaA = createMetaWithDeps("pluginA", "1.0.0", Map.of("pluginB", "^2.1.0"));
            var metaB = createMeta("pluginB", "3.0.0");

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
    private static class DummyGameProvider implements GameProvider {
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

    /**
     * Valid plugin stub entrypoint for ClassLoader instantiation tests.
     */
    public static final class ValidStubPlugin implements Plugin {
        public static PluginData initializedData;

        @Override
        public void onInitialize(PluginData pluginData) {
            initializedData = pluginData;
        }
    }

    /**
     * Invalid stub entrypoint that does not implement {@link Plugin}.
     */
    public static final class NonPluginClass {
        public NonPluginClass() {
        }
    }
}