package com.avrix.plugins;

import com.avrix.core.Environment;
import com.avrix.core.Metadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("AvrixPluginManager.resolvePluginLoadOrder")
class ResolvePluginLoadOrderTest {

    private AvrixPluginManager manager;

    @BeforeEach
    void setUp() throws Exception {
        manager = new AvrixPluginManager();
        clearCorePlugins();
    }

    @Nested
    @DisplayName("Version Constraint Validation")
    class VersionConstraints {

        @ParameterizedTest(name = "Constraint ''{0}'' vs version ''{1}'' → should pass")
        @CsvSource({
                "'1.0.0', '1.0.0'",
                "'>=1.0.0', '1.0.0'",
                "'>=1.0.0', '2.5.3'",
                "'<2.0.0', '1.9.9'",
                "'<=1.5.0', '1.5.0'",
                "'>1.0.0', '1.0.1'",
                "'>=1.0.0 && <2.0.0', '1.5.0'",
                "'~1.2.0', '1.2.9'",
                "'^1.2.0', '1.9.9'",
                "'*', '9.9.9'",
                "'1.x', '1.5.0'",
                "'>=1.0.0 || >=2.0.0', '2.1.0'"
        })
        @DisplayName("Valid constraints resolve successfully")
        void validConstraints_shouldPass(String constraint, String actualVersion) {
            var lib = createMeta("lib", actualVersion);
            var plugin = createMeta("plugin", "1.0.0", Map.of("lib", constraint));

            var result = manager.resolvePluginLoadOrder(List.of(lib, plugin));

            assertThat(result).extracting(Metadata::getId).containsExactly("lib", "plugin");
        }

        @ParameterizedTest(name = "Constraint ''{0}'' vs version ''{1}'' → should fail")
        @CsvSource({
                "'1.0.0', '1.0.1'",
                "'>=2.0.0', '1.9.9'",
                "'<1.0.0', '1.0.0'",
                "'<=1.5.0', '1.6.0'",
                "'>=1.0.0 && <2.0.0', '2.0.0'",
                "'~1.2.0', '1.3.0'",
                "'^1.2.0', '2.0.0'",
                "'1.x', '2.0.0'"
        })
        @DisplayName("Mismatched constraints throw IllegalStateException")
        void mismatchedConstraints_shouldFail(String constraint, String actualVersion) {
            var lib = createMeta("lib", actualVersion);
            var plugin = createMeta("plugin", "1.0.0", Map.of("lib", constraint));

            assertThatThrownBy(() -> manager.resolvePluginLoadOrder(List.of(lib, plugin)))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("requires 'lib' matching")
                    .hasMessageContaining("but found '" + actualVersion + "'");
        }

        @Test
        @DisplayName("Invalid constraint syntax throws IllegalStateException")
        void invalidConstraintSyntax_shouldFail() {
            var lib = createMeta("lib", "1.0.0");
            var plugin = createMeta("plugin", "1.0.0", Map.of("lib", ">>invalid<<"));

            assertThatThrownBy(() -> manager.resolvePluginLoadOrder(List.of(lib, plugin)))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Invalid version/constraint");
        }

        @Test
        @DisplayName("Non-SemVer actual version throws IllegalStateException")
        void invalidActualVersion_shouldFail() {
            var lib = createMeta("lib", "not-semver");
            var plugin = createMeta("plugin", "1.0.0", Map.of("lib", ">=1.0.0"));

            assertThatThrownBy(() -> manager.resolvePluginLoadOrder(List.of(lib, plugin)))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Invalid version/constraint");
        }
    }

    @Nested
    @DisplayName("Topological Sorting & Graph Logic")
    class GraphLogic {

        @Test
        @DisplayName("Linear chain: C → B → A resolves to [C, B, A]")
        void linearChain() {
            var c = createMeta("C", "1.0.0");
            var b = createMeta("B", "1.0.0", Map.of("C", ">=1.0.0"));
            var a = createMeta("A", "1.0.0", Map.of("B", ">=1.0.0"));

            var result = manager.resolvePluginLoadOrder(List.of(a, b, c));

            assertThat(result).extracting(Metadata::getId).containsExactly("C", "B", "A");
        }

        @Test
        @DisplayName("Diamond dependency resolves correctly")
        void diamondDependency() {
            var a = createMeta("A", "1.0.0");
            var b = createMeta("B", "1.0.0", Map.of("A", ">=1.0.0"));
            var c = createMeta("C", "1.0.0", Map.of("A", ">=1.0.0"));
            var d = createMeta("D", "1.0.0", Map.of("B", ">=1.0.0", "C", ">=1.0.0"));

            var result = manager.resolvePluginLoadOrder(List.of(a, b, c, d));

            assertThat(result).hasSize(4);
            assertThat(result.get(0).getId()).isEqualTo("A");
            assertThat(result.get(result.size() - 1).getId()).isEqualTo("D");
        }

        @Test
        @DisplayName("Independent plugins retain all entries")
        void independentPlugins() {
            var a = createMeta("A", "1.0.0");
            var b = createMeta("B", "2.0.0");

            var result = manager.resolvePluginLoadOrder(List.of(a, b));

            assertThat(result).extracting(Metadata::getId).containsExactlyInAnyOrder("A", "B");
        }
    }

    @Nested
    @DisplayName("Edge Cases & Error Handling")
    class EdgeCases {

        @Test
        @DisplayName("Circular dependency throws IllegalStateException")
        void circularDependency() {
            var a = createMeta("A", "1.0.0", Map.of("B", "1.0.0"));
            var b = createMeta("B", "1.0.0", Map.of("A", "1.0.0"));

            assertThatThrownBy(() -> manager.resolvePluginLoadOrder(List.of(a, b)))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Circular plugin dependency");
        }

        @Test
        @DisplayName("Missing dependency throws IllegalStateException")
        void missingDependency() {
            var plugin = createMeta("plugin", "1.0.0", Map.of("missing-lib", ">=1.0.0"));

            assertThatThrownBy(() -> manager.resolvePluginLoadOrder(List.of(plugin)))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("requires missing dependency 'missing-lib'");
        }

        @Test
        @DisplayName("Duplicate IDs: first occurrence wins")
        void duplicatePluginId() {
            var v1 = createMeta("plugin", "1.0.0");
            var v2 = createMeta("plugin", "2.0.0");

            var result = manager.resolvePluginLoadOrder(List.of(v1, v2));

            assertThat(result).hasSize(1)
                    .first().extracting(Metadata::getVersion).isEqualTo("1.0.0");
        }

        @Test
        @DisplayName("Empty input returns empty list")
        void emptyInput() {
            assertThat(manager.resolvePluginLoadOrder(List.of())).isEmpty();
        }
    }

    @Nested
    @DisplayName("Core Plugin Filtering")
    class CoreFiltering {

        @Test
        @DisplayName("Core plugins are excluded from the result")
        void excludeCorePlugins() throws Exception {
            var core = createMeta("core", "1.0.0");
            var user = createMeta("user", "1.0.0", Map.of("core", ">=1.0.0"));
            injectCorePlugin(core);

            var result = manager.resolvePluginLoadOrder(List.of(core, user));

            assertThat(result).extracting(Metadata::getId).containsExactly("user");
        }

        @Test
        @DisplayName("Dependencies on core plugins are validated but core is excluded")
        void dependOnCore_excludedFromResult() throws Exception {
            var core = createMeta("framework", "3.0.0");
            var plugin = createMeta("plugin", "1.0.0", Map.of("framework", ">=3.0.0"));
            injectCorePlugin(core);

            var result = manager.resolvePluginLoadOrder(List.of(plugin, core));

            assertThat(result).extracting(Metadata::getId).containsExactly("plugin");
        }
    }

    // === Test Utilities ===

    private Metadata createMeta(String id, String version) {
        return createMeta(id, version, Map.of());
    }

    private Metadata createMeta(String id, String version, Map<String, String> deps) {
        return new Metadata.Builder()
                .schema(1)
                .id(id)
                .name(id)
                .version(version)
                .environment(Environment.BOTH)
                .dependencies(deps)
                .build();
    }

    private void clearCorePlugins() throws Exception {
        Field field = AvrixPluginManager.class.getDeclaredField("CORE_PLUGINS");
        field.setAccessible(true);
        ((List<?>) field.get(manager)).clear();
    }

    private void injectCorePlugin(Metadata meta) throws Exception {
        Field field = AvrixPluginManager.class.getDeclaredField("CORE_PLUGINS");
        field.setAccessible(true);
        ((List<Metadata>) field.get(manager)).add(meta);
    }
}