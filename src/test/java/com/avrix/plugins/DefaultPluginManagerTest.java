package com.avrix.plugins;

import com.avrix.core.Environment;
import com.avrix.core.KnotClassLoader;
import com.avrix.core.LoaderConfig;
import com.avrix.core.Metadata;
import com.avrix.core.MixinRegistrar;
import com.avrix.provider.DefaultLoaderProvider;
import com.avrix.provider.ZomboidModPluginSource;
import com.avrix.provider.ZomboidPluginApprovalGate;
import com.avrix.api.GameProvider;
import com.avrix.api.LoaderProvider;
import com.avrix.utils.Constants;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import test.plugin.ErrorThrowingPlugin;
import test.plugin.LifecyclePlugin;
import test.plugin.PhaseAEntrypoint;
import test.plugin.PhaseTracker;

import java.io.File;
import java.io.IOException;
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
import static org.assertj.core.groups.Tuple.tuple;

/**
 * Unit test suite for {@link DefaultPluginManager} dependency graph resolution,
 * soft load ordering rules, and lifecycle operations.
 */
@DisplayName("DefaultPluginManager Unit Tests")
class DefaultPluginManagerTest {

    @TempDir
    Path tempRoot;

    private final MixinRegistrar mixinRegistrar = mixinClass -> {};

    private DefaultPluginManager manager;
    private Path pluginsDirectory;
    private KnotClassLoader classLoader;
    private GameProvider gameProvider;
    private LoaderProvider loaderProvider;
    private ClassLoader originalContextClassLoader;
    private static final int SCHEMA = Constants.METADATA_SCHEMA;

    @BeforeEach
    void setUp() {
        originalContextClassLoader = Thread.currentThread().getContextClassLoader();
        pluginsDirectory = tempRoot.resolve("plugins");
        classLoader = new KnotClassLoader(new URL[0], ClassLoader.getSystemClassLoader());
        Thread.currentThread().setContextClassLoader(classLoader);

        gameProvider = new DummyGameProvider();
        loaderProvider = new DefaultLoaderProvider();

        manager = createManager(configWithEnvironment(Environment.BOTH), gameProvider);
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

    private LoaderConfig configWithEnvironment(Environment environment) {
        return new LoaderConfig(
                pluginsDirectory,
                Constants.METADATA_NAME,
                Constants.PLUGIN_EXTENSION,
                Constants.PLUGIN_ICON_NAME,
                Constants.AUTO_PROVIDER_ID,
                environment,
                true,
                false
        );
    }

    private DefaultPluginManager createManager(LoaderConfig config, GameProvider provider) {
        return new DefaultPluginManager(
                config,
                classLoader,
                provider,
                loaderProvider,
                mixinRegistrar,
                new PluginScanner(config),
                new PluginManifestParser()
        );
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

    private void createPluginJarOnDisk(Path destination, Metadata metadata, Class<?>... classes) throws Exception {
        try (var jos = new JarOutputStream(Files.newOutputStream(destination))) {
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

            var entry = new JarEntry(Constants.METADATA_NAME);
            jos.putNextEntry(entry);
            String yaml = """
                    schema: %d
                    id: %s
                    name: %s
                    version: %s
                    environment: %s
                    entrypoint: %s
                    """.formatted(
                    metadata.schema(),
                    metadata.id(),
                    metadata.name(),
                    metadata.version(),
                    metadata.environment().name().toLowerCase(),
                    metadata.entrypoint()
            );

            if (!metadata.mixins().isEmpty()) {
                StringBuilder yamlWithMixins = new StringBuilder(yaml);
                yamlWithMixins.append("mixins:\n");
                for (String mixin : metadata.mixins()) {
                    yamlWithMixins.append("  - \"").append(mixin).append("\"\n");
                }
                yaml = yamlWithMixins.toString();
            }

            if (!metadata.conflicts().isEmpty()) {
                StringBuilder yamlWithConflicts = new StringBuilder(yaml);
                yamlWithConflicts.append("conflicts:\n");
                for (String conflict : metadata.conflicts()) {
                    yamlWithConflicts.append("  - \"").append(conflict).append("\"\n");
                }
                yaml = yamlWithConflicts.toString();
            }

            jos.write(yaml.getBytes(StandardCharsets.UTF_8));
            jos.closeEntry();
        }
    }

    @Nested
    @DisplayName("Dependency Resolution & Topological Sorting")
    class ResolutionTests {

        @Test
        @DisplayName("Should sort independent plugins without ordering constraints")
        void shouldSortIndependentPlugins() {
            var meta1 = createMeta("plugina", "1.0.0");
            var meta2 = createMeta("pluginb", "1.0.0");
            var meta3 = createMeta("pluginc", "1.0.0");

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
            var metaA = createMetaWithDeps("plugina", "1.0.0", Map.of("pluginb", ">=1.0.0"));
            var metaB = createMetaWithDeps("pluginb", "1.0.0", Map.of("pluginc", ">=1.0.0"));
            var metaC = createMeta("pluginc", "1.0.0");

            var result = manager.resolvePluginLoadOrder(List.of(metaA, metaC, metaB));

            assertThat(result)
                    .extracting(Metadata::id)
                    .containsExactly("pluginc", "pluginb", "plugina");
        }

        @Test
        @DisplayName("Should resolve and sort complex Directed Acyclic Graph (DAG)")
        void shouldSortComplexGraph() {
            // A depends on B and C; B and C depend on D
            // Expected load order: D, (B/C in any order), A
            var metaA = createMetaWithDeps("a", "1.0.0", Map.of("b", ">=1.0.0", "c", ">=1.0.0"));
            var metaB = createMetaWithDeps("b", "1.0.0", Map.of("d", ">=1.0.0"));
            var metaC = createMetaWithDeps("c", "1.0.0", Map.of("d", ">=1.0.0"));
            var metaD = createMeta("d", "1.0.0");

            var result = manager.resolvePluginLoadOrder(List.of(metaA, metaB, metaC, metaD));

            assertThat(result).hasSize(4);
            assertThat(result.get(0).id()).isEqualTo("d");
            assertThat(result.get(3).id()).isEqualTo("a");
            assertThat(result.subList(1, 3)).extracting(Metadata::id).containsExactlyInAnyOrder("b", "c");
        }

        @Test
        @DisplayName("Should throw IllegalStateException when a required dependency is missing")
        void shouldFailOnMissingDependency() {
            var metaA = createMetaWithDeps("plugina", "1.0.0", Map.of("missing-plugin", ">=1.0.0"));

            assertThatThrownBy(() -> manager.resolvePluginLoadOrder(List.of(metaA)))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Plugin [plugina] depends on missing component [missing-plugin]");
        }

        @Test
        @DisplayName("Should detect and reject cyclic plugin dependency graphs")
        void shouldFailOnCircularDependency() {
            // A -> B -> C -> A
            var metaA = createMetaWithDeps("a", "1.0.0", Map.of("b", ">=1.0.0"));
            var metaB = createMetaWithDeps("b", "1.0.0", Map.of("c", ">=1.0.0"));
            var metaC = createMetaWithDeps("c", "1.0.0", Map.of("a", ">=1.0.0"));

            assertThatThrownBy(() -> manager.resolvePluginLoadOrder(List.of(metaA, metaB, metaC)))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cyclic plugin dependency or load order detected")
                    .hasMessageContaining("a")
                    .hasMessageContaining("b")
                    .hasMessageContaining("c");
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
                    .id("plugina")
                    .name("A")
                    .version("1.0.0")
                    .loadAfter("pluginb", "optional-missing-plugin")
                    .build();
            var metaB = createMeta("pluginb", "1.0.0");

            var result = manager.resolvePluginLoadOrder(List.of(metaA, metaB));

            assertThat(result)
                    .extracting(Metadata::id)
                    .containsExactly("pluginb", "plugina");
        }

        @Test
        @DisplayName("Should respect direct loadBefore constraints")
        void shouldRespectLoadBefore() {
            // Plugin A specifies loadBefore: [B]
            // Expected load order: A, B
            var metaA = new Metadata.Builder()
                    .schema(SCHEMA)
                    .id("plugina")
                    .name("A")
                    .version("1.0.0")
                    .loadBefore("pluginb")
                    .build();
            var metaB = createMeta("pluginb", "1.0.0");

            var result = manager.resolvePluginLoadOrder(List.of(metaB, metaA));

            assertThat(result)
                    .extracting(Metadata::id)
                    .containsExactly("plugina", "pluginb");
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
            var metaA = new Metadata.Builder().schema(SCHEMA).id("a").name("A").version("1.0.0").loadBefore("b").build();
            var metaB = new Metadata.Builder().schema(SCHEMA).id("b").name("B").version("1.0.0").loadBefore("a").build();

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
            var p1 = new Metadata.Builder().schema(SCHEMA).id("p1").name("P1").version("1.0.0").loadBefore("p2").build();
            var p2 = new Metadata.Builder().schema(SCHEMA).id("p2").name("P2").version("1.0.0").loadBefore("p3").build();
            var p3 = new Metadata.Builder().schema(SCHEMA).id("p3").name("P3").version("1.0.0").loadBefore("p4").build();
            var p4 = new Metadata.Builder().schema(SCHEMA).id("p4").name("P4").version("1.0.0").loadBefore("p5").build();
            var p5 = new Metadata.Builder().schema(SCHEMA).id("p5").name("P5").version("1.0.0").loadBefore("p1").build();

            assertThatThrownBy(() -> manager.resolvePluginLoadOrder(List.of(p3, p1, p5, p2, p4)))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cyclic plugin dependency or load order detected")
                    .hasMessageContaining("p1")
                    .hasMessageContaining("p5");
        }

        @Test
        @DisplayName("Should resolve two wildcard plugins when disambiguated by explicit loadAfter rule")
        void shouldDisambiguateMultipleWildcardPlugins() {
            var plugina = new Metadata.Builder()
                    .schema(SCHEMA).id("plugin-a").name("A").version("1.0.0")
                    .loadBefore("*")
                    .build();

            var pluginb = new Metadata.Builder()
                    .schema(SCHEMA).id("plugin-b").name("B").version("1.0.0")
                    .loadBefore("*")
                    .loadAfter("plugin-a")
                    .build();

            var normal1 = createMeta("normal-1", "1.0.0");
            var normal2 = createMeta("normal-2", "1.0.0");

            var sorted = manager.resolvePluginLoadOrder(List.of(normal2, pluginb, normal1, plugina));
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
        @DisplayName("Should reject duplicate plugin IDs")
        void shouldRejectDuplicatePluginIds() {
            var metaFirst = new Metadata.Builder().schema(SCHEMA).id("dup-id").name("First").version("1.0.0").build();
            var metaSecond = new Metadata.Builder().schema(SCHEMA).id("dup-id").name("Second").version("2.0.0").build();
            var metaOther = createMeta("other", "1.0.0");

            assertThatThrownBy(() -> manager.resolvePluginLoadOrder(List.of(metaFirst, metaSecond, metaOther)))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Duplicate plugin identifier [dup-id]");
        }

        @Test
        @DisplayName("Should reject plugin IDs reserved for core components")
        void shouldRejectReservedPluginIds() {
            assertThatThrownBy(() -> manager.resolvePluginLoadOrder(List.of(createMeta(Constants.LOADER_ID, "1.0.0"))))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("reserved for a core component");
        }

        @Test
        @DisplayName("Should produce a deterministic order for independent plugins")
        void shouldProduceDeterministicOrderForIndependentPlugins() {
            var pluginZ = createMeta("plugin-z", "1.0.0");
            var plugina = createMeta("plugin-a", "1.0.0");
            var pluginM = createMeta("plugin-m", "1.0.0");

            var result = manager.resolvePluginLoadOrder(List.of(pluginZ, plugina, pluginM));

            assertThat(result).extracting(Metadata::id).containsExactly("plugin-a", "plugin-m", "plugin-z");
        }

        @Test
        @DisplayName("Should reject a self-referencing dependency")
        void shouldDetectSelfCycle() {
            var selfDep = createMetaWithDeps("self-dep", "1.0.0", Map.of("self-dep", ">=1.0.0"));

            assertThatThrownBy(() -> manager.resolvePluginLoadOrder(List.of(selfDep)))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Plugin [self-dep] cannot depend on itself");
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
            assertThat(loaded.getPluginIconURI()).isEmpty();
        }

        @Test
        @DisplayName("Should expose an icon URI only when the archive contains the icon")
        void shouldExposeIconOnlyWhenPresent() throws Exception {
            Path jarPath = tempDir.resolve("icon-plugin.jar");
            try (var jos = new JarOutputStream(Files.newOutputStream(jarPath))) {
                jos.putNextEntry(new JarEntry(Constants.PLUGIN_ICON_NAME));
                jos.write(new byte[]{1, 2, 3});
                jos.closeEntry();
            }

            var meta = new Metadata.Builder()
                    .schema(SCHEMA)
                    .id("icon-plugin")
                    .name("Icon Plugin")
                    .version("1.0.0")
                    .build();

            manager.loadPlugin(new PluginData(jarPath.toFile(), null, null, meta));

            assertThat(manager.getPlugins().get("icon-plugin").getPluginIconURI()).isPresent();
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
            assertThat(ValidStubPlugin.initializedData.getPluginInstance()).isPresent();
            assertThat(manager.getLoadResults())
                    .extracting(PluginLoadResult::pluginId, PluginLoadResult::status)
                    .containsExactly(tuple("exec-plugin", PluginLoadStatus.LOADED));
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
        @DisplayName("Should invoke plugin shutdown callback when manager is closed")
        void shouldInvokeShutdownCallback() throws Exception {
            PhaseTracker.reset();

            var jarFile = createDummyJar("lifecycle-plugin.jar", LifecyclePlugin.class);
            var meta = new Metadata.Builder()
                    .schema(SCHEMA)
                    .id("lifecycle-plugin")
                    .name("Lifecycle Plugin")
                    .version("1.0.0")
                    .entrypoint(LifecyclePlugin.class.getName())
                    .build();

            manager.loadPlugin(new PluginData(jarFile, null, null, meta));
            manager.close();

            try {
                assertThat(PhaseTracker.events())
                        .containsExactly("init:lifecycle", "shutdown:lifecycle");
                assertThat(manager.getPlugins()).isEmpty();
            } finally {
                PhaseTracker.reset();
            }
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

            assertThatThrownBy(() -> pluginsMap.put("illegal", new PluginData(createMeta("illegal", "1.0.0"))))
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

        @Test
        @DisplayName("Should throw IllegalStateException when loadPlugins() is called before init()")
        void shouldThrowWhenNotInitialized() {
            var uninitializedManager = createManager(configWithEnvironment(Environment.BOTH), gameProvider);
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
            // Setup a game provider running specifically in CLIENT environment
            GameProvider clientProvider = new DummyGameProvider() {
                /** {@inheritDoc} */
                @Override
                public Environment getEnvironment() {
                    return Environment.CLIENT;
                }
            };

            var envManager = createManager(configWithEnvironment(Environment.CLIENT), clientProvider);
            envManager.init();

            Path pluginsPath = pluginsDirectory;
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
            Path pluginsPath = pluginsDirectory;
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

        @Test
        @DisplayName("Should skip plugins listed in disabled.txt")
        void shouldSkipDisabledPlugins() throws Exception {
            manager.init();
            Path pluginsPath = pluginsDirectory;
            Files.createDirectories(pluginsPath);

            var disabledMeta = createMeta("disabled-plugin", "1.0.0");
            createPluginJarOnDisk(pluginsPath.resolve("disabled-plugin.jar"), disabledMeta);

            var filenameDisabledMeta = createMeta("filename-disabled-plugin", "1.0.0");
            createPluginJarOnDisk(pluginsPath.resolve("filename-disabled-plugin.jar"), filenameDisabledMeta);

            var enabledMeta = createMeta("enabled-plugin", "1.0.0");
            createPluginJarOnDisk(pluginsPath.resolve("enabled-plugin.jar"), enabledMeta);

            Files.writeString(pluginsPath.resolve(Constants.DISABLED_PLUGINS_FILE_NAME), """
                    # Disabled plugins
                    disabled-plugin
                    filename-disabled-plugin.jar
                    """);

            manager.loadPlugins();

            assertThat(manager.getPlugins()).containsKey("enabled-plugin");
            assertThat(manager.getPlugins())
                    .doesNotContainKeys("disabled-plugin", "filename-disabled-plugin");
            assertThat(manager.getLoadResults())
                    .extracting(PluginLoadResult::pluginId, PluginLoadResult::status)
                    .contains(
                            tuple("disabled-plugin", PluginLoadStatus.SKIPPED),
                            tuple("filename-disabled-plugin.jar", PluginLoadStatus.SKIPPED)
                    );
        }

        @Test
        @DisplayName("Should isolate plugin errors and report failed plugins")
        void shouldIsolatePluginErrorsAndReportResults() throws Exception {
            manager.init();
            Path pluginsPath = pluginsDirectory;
            Files.createDirectories(pluginsPath);

            var failingMeta = new Metadata.Builder()
                    .schema(SCHEMA)
                    .id("failing-plugin")
                    .name("Failing Plugin")
                    .version("1.0.0")
                    .entrypoint(ErrorThrowingPlugin.class.getName())
                    .build();
            createPluginJarOnDisk(pluginsPath.resolve("failing-plugin.jar"), failingMeta, ErrorThrowingPlugin.class);

            var validMeta = createMeta("valid-plugin", "1.0.0");
            createPluginJarOnDisk(pluginsPath.resolve("valid-plugin.jar"), validMeta);

            manager.loadPlugins();

            assertThat(manager.getPlugins()).containsKey("valid-plugin");
            assertThat(manager.getPlugins()).doesNotContainKey("failing-plugin");
            assertThat(manager.getLoadResults())
                    .extracting(PluginLoadResult::pluginId, PluginLoadResult::status)
                    .contains(
                            tuple("valid-plugin", PluginLoadStatus.LOADED),
                            tuple("failing-plugin", PluginLoadStatus.FAILED)
                    );
        }

        @Test
        @DisplayName("Should abort plugin loading in strict mode")
        void shouldAbortInStrictMode() throws Exception {
            var strictManager = createManager(
                    configWithEnvironment(Environment.BOTH).withStrict(true),
                    gameProvider
            );
            strictManager.init();

            Path pluginsPath = pluginsDirectory;
            Files.createDirectories(pluginsPath);

            var failingMeta = new Metadata.Builder()
                    .schema(SCHEMA)
                    .id("failing-plugin")
                    .name("Failing Plugin")
                    .version("1.0.0")
                    .entrypoint(ErrorThrowingPlugin.class.getName())
                    .build();
            createPluginJarOnDisk(pluginsPath.resolve("failing-plugin.jar"), failingMeta, ErrorThrowingPlugin.class);

            assertThatThrownBy(strictManager::loadPlugins)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Plugin load failed: failing-plugin");
        }

        @Test
        @DisplayName("Should register all mixins before starting plugin entrypoints")
        void shouldPrepareAllPluginsBeforeStartingEntrypoints() throws Exception {
            PhaseTracker.reset();

            var config = configWithEnvironment(Environment.BOTH);
            var twoPhaseManager = new DefaultPluginManager(
                    config,
                    classLoader,
                    gameProvider,
                    loaderProvider,
                    mixin -> PhaseTracker.record("mixin:" + mixin),
                    new PluginScanner(config),
                    new PluginManifestParser()
            );
            twoPhaseManager.init();

            Path pluginsPath = pluginsDirectory;
            Files.createDirectories(pluginsPath);

            var metaA = new Metadata.Builder()
                    .schema(SCHEMA)
                    .id("plugin-a")
                    .name("Plugin A")
                    .version("1.0.0")
                    .entrypoint(PhaseAEntrypoint.class.getName())
                    .build();
            createPluginJarOnDisk(pluginsPath.resolve("plugin-a.jar"), metaA, PhaseAEntrypoint.class);

            var metaB = new Metadata.Builder()
                    .schema(SCHEMA)
                    .id("plugin-b")
                    .name("Plugin B")
                    .version("1.0.0")
                    .mixins("test.plugin.PhaseBMixin")
                    .build();
            createPluginJarOnDisk(pluginsPath.resolve("plugin-b.jar"), metaB);

            try {
                twoPhaseManager.loadPlugins();

                assertThat(PhaseTracker.events())
                        .containsExactly("mixin:test.plugin.PhaseBMixin", "init:plugin-a");
            } finally {
                PhaseTracker.reset();
            }
        }

    }

    @Test
    @DisplayName("Should prefer a mod archive over the plugins directory when plugin IDs collide")
    void shouldPreferModArchiveOverPluginsDirectoryForDuplicateId() throws Exception {
        Metadata metadata = createMeta("duplicate-plugin", "1.0.0");
        Files.createDirectories(pluginsDirectory);
        createPluginJarOnDisk(pluginsDirectory.resolve("folder-plugin.jar"), metadata);

        Path gameRoot = tempRoot.resolve("steamapps/common/ProjectZomboid");
        Files.createDirectories(gameRoot);

        Path userHome = tempRoot.resolve("home");
        Path versionDirectory = userHome.resolve("Zomboid/mods/TestMod/42");
        Files.createDirectories(versionDirectory.resolve("media/java"));
        Files.writeString(userHome.resolve("Zomboid/mods/default.txt"), """
                mods
                {
                    mod = test-mod,
                }
                """);
        Files.writeString(versionDirectory.resolve("mod.info"), """
                id=test-mod
                jarPlugins=media/java/mod-plugin.jar
                """);

        Path modJar = versionDirectory.resolve("media/java/mod-plugin.jar");
        createPluginJarOnDisk(modJar, metadata);

        LoaderConfig config = configWithEnvironment(Environment.BOTH);
        DefaultPluginManager priorityManager = new DefaultPluginManager(
                config,
                classLoader,
                gameProvider,
                loaderProvider,
                mixinRegistrar,
                new PluginScanner(
                        config,
                        List.of(new ZomboidModPluginSource(gameRoot, userHome, Environment.BOTH, null))
                ),
                new PluginManifestParser()
        );
        priorityManager.init();
        priorityManager.loadPlugins();

        assertThat(priorityManager.getPlugins()).containsKey("duplicate-plugin");
        assertThat(priorityManager.getPlugins().get("duplicate-plugin").getPluginFile().orElseThrow())
                .isEqualTo(modJar.toFile());
    }

    @Test
    @DisplayName("Should skip a mod plugin while approval is pending")
    void shouldSkipPendingModPlugin() throws Exception {
        Path gameRoot = tempRoot.resolve("steamapps/common/ProjectZomboid");
        Files.createDirectories(gameRoot);

        Path userHome = tempRoot.resolve("home");
        Path versionDirectory = userHome.resolve("Zomboid/mods/ApprovalMod/42");
        Files.createDirectories(versionDirectory.resolve("media/java"));
        Files.writeString(userHome.resolve("Zomboid/mods/default.txt"), """
                mods
                {
                    mod = approval-mod,
                }
                """);
        Files.writeString(versionDirectory.resolve("mod.info"), """
                id=approval-mod
                jarPlugins=media/java/approval-plugin.jar
                """);

        Metadata metadata = createMeta("approval-plugin", "1.0.0");
        createPluginJarOnDisk(versionDirectory.resolve("media/java/approval-plugin.jar"), metadata);

        LoaderConfig config = configWithEnvironment(Environment.BOTH);
        ZomboidPluginApprovalGate approvalService = new ZomboidPluginApprovalGate(
                gameRoot, userHome.resolve("Zomboid"), Environment.BOTH, "prompt"
        );
        DefaultPluginManager approvalManager = new DefaultPluginManager(
                config,
                classLoader,
                gameProvider,
                loaderProvider,
                mixinRegistrar,
                new PluginScanner(
                        config,
                        List.of(new ZomboidModPluginSource(gameRoot, userHome, Environment.BOTH, null))
                ),
                new PluginManifestParser(),
                approvalService
        );
        approvalManager.init();
        approvalManager.loadPlugins();

        Path pendingFile = gameRoot.resolve("avrix/plugin-pending.txt");
        assertThat(approvalManager.getPlugins()).doesNotContainKey("approval-plugin");
        assertThat(pendingFile).isRegularFile();
        assertThat(Files.readString(pendingFile)).contains("ENTRY", "approval-mod", "approval-plugin");
    }

    @Nested
    @DisplayName("Conflict Resolution Tests")
    class ConflictTests {

        @Test
        @DisplayName("Should load conflicting plugins with a warning in non-strict mode")
        void shouldLoadConflictingPluginsInNonStrictMode() throws Exception {
            manager.init();
            Files.createDirectories(pluginsDirectory);

            var plugina = new Metadata.Builder()
                    .schema(SCHEMA)
                    .id("conflict-a")
                    .name("Conflict A")
                    .version("1.0.0")
                    .conflicts("conflict-b")
                    .build();
            var pluginb = new Metadata.Builder()
                    .schema(SCHEMA)
                    .id("conflict-b")
                    .name("Conflict B")
                    .version("1.0.0")
                    .build();

            createPluginJarOnDisk(pluginsDirectory.resolve("conflict-a.jar"), plugina);
            createPluginJarOnDisk(pluginsDirectory.resolve("conflict-b.jar"), pluginb);

            manager.loadPlugins();

            assertThat(manager.getPlugins()).containsKeys("conflict-a", "conflict-b");
        }

        @Test
        @DisplayName("Should abort on conflicting plugins in strict mode")
        void shouldAbortOnConflictingPluginsInStrictMode() throws Exception {
            var strictManager = createManager(configWithEnvironment(Environment.BOTH).withStrict(true), gameProvider);
            strictManager.init();
            Files.createDirectories(pluginsDirectory);

            var plugina = new Metadata.Builder()
                    .schema(SCHEMA)
                    .id("conflict-a")
                    .name("Conflict A")
                    .version("1.0.0")
                    .conflicts("conflict-b")
                    .build();
            var pluginb = new Metadata.Builder()
                    .schema(SCHEMA)
                    .id("conflict-b")
                    .name("Conflict B")
                    .version("1.0.0")
                    .build();

            createPluginJarOnDisk(pluginsDirectory.resolve("conflict-a.jar"), plugina);
            createPluginJarOnDisk(pluginsDirectory.resolve("conflict-b.jar"), pluginb);

            assertThatThrownBy(strictManager::loadPlugins)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Failed to resolve plugin load order")
                    .hasRootCauseMessage("Plugin [conflict-a] conflicts with installed plugin [conflict-b]");
        }
    }

    @Nested
    @DisplayName("SemVer Constraint Verification")
    class SemVerTests {

        @Test
        @DisplayName("Should accept valid SemVer caret constraints")
        void shouldAcceptValidConstraints() {
            var metaA = createMetaWithDeps("plugina", "1.0.0", Map.of("pluginb", "^2.1.0"));
            var metaB = createMeta("pluginb", "2.1.5");

            var result = manager.resolvePluginLoadOrder(List.of(metaA, metaB));

            assertThat(result).extracting(Metadata::id).containsExactly("pluginb", "plugina");
        }

        @Test
        @DisplayName("Should reject incompatible SemVer versions")
        void shouldRejectInvalidConstraints() {
            var metaA = createMetaWithDeps("plugina", "1.0.0", Map.of("pluginb", "^2.1.0"));
            var metaB = createMeta("pluginb", "3.0.0");

            assertThatThrownBy(() -> manager.resolvePluginLoadOrder(List.of(metaA, metaB)))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Plugin [plugina] requires [pluginb] matching [^2.1.0], but found version [3.0.0]");
        }

        @Test
        @DisplayName("Should reject a malformed version while building metadata")
        void shouldFailOnMalformedDependencyVersion() {
            assertThatThrownBy(() -> createMeta("pluginb", "invalid-semver-string"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid plugin version");
        }

        @Test
        @DisplayName("Should reject an invalid version constraint while building metadata")
        void shouldFailOnMalformedConstraint() {
            assertThatThrownBy(() -> createMetaWithDeps("plugina", "1.0.0", Map.of("pluginb", "not-a-valid-range")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Dependency [pluginb] has invalid version constraint");
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

    /**
     * Valid plugin stub entrypoint for ClassLoader instantiation tests.
     */
    public static final class ValidStubPlugin implements Plugin {
        /**
         * Captures the descriptor passed to the stub entrypoint.
         */
        public static PluginData initializedData;

        /** {@inheritDoc} */
        @Override
        public void onInitialize(PluginData pluginData) {
            initializedData = pluginData;
        }
    }

    /**
     * Invalid stub entrypoint that does not implement {@link Plugin}.
     */
    public static final class NonPluginClass {
        /**
         * Constructs a non-plugin stub for negative entrypoint tests.
         */
        public NonPluginClass() {
        }
    }
}