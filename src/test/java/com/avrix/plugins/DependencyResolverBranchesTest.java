package com.avrix.plugins;

import com.avrix.core.Environment;
import com.avrix.core.Metadata;
import com.avrix.utils.Constants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Branch-focused unit tests for {@link DependencyResolver} wildcard disambiguation and cycle reporting.
 */
@DisplayName("DependencyResolver Branch Tests")
class DependencyResolverBranchesTest {

    private static final int SCHEMA = Constants.METADATA_SCHEMA;

    @Test
    @DisplayName("Should ignore a loadBefore target that is not part of the candidate set")
    void shouldIgnoreMissingLoadBeforeTarget() {
        var resolver = new DependencyResolver(List.of());
        var plugin = builder("plugin", "1.0.0").loadBefore("missing-target").build();

        var result = resolver.resolve(List.of(plugin));

        assertThat(result).extracting(Metadata::id).containsExactly("plugin");
    }

    @Test
    @DisplayName("Should yield to an explicit reverse loadBefore rule over a wildcard loadBefore")
    void shouldYieldToExplicitReverseLoadBefore() {
        var resolver = new DependencyResolver(List.of());
        var wildcard = builder("wildcard", "1.0.0").loadBefore("*").build();
        var explicit = builder("explicit", "1.0.0").loadBefore("wildcard").build();

        var result = resolver.resolve(List.of(wildcard, explicit));

        assertThat(result).extracting(Metadata::id).containsExactly("explicit", "wildcard");
    }

    @Test
    @DisplayName("Should avoid a cycle when two plugins both declare loadBefore wildcard")
    void shouldDisambiguateMutualWildcardLoadBefore() {
        var resolver = new DependencyResolver(List.of());
        var first = builder("a-wild", "1.0.0").loadBefore("*").build();
        var second = builder("b-wild", "1.0.0").loadBefore("*").build();
        var regular = meta("m-regular", "1.0.0");

        var result = resolver.resolve(List.of(second, regular, first));

        assertThat(result).extracting(Metadata::id).containsExactly("a-wild", "b-wild", "m-regular");
    }

    @Test
    @DisplayName("Should yield to an explicit reverse loadAfter rule over a wildcard loadAfter")
    void shouldYieldToExplicitReverseLoadAfter() {
        var resolver = new DependencyResolver(List.of());
        var late = builder("late", "1.0.0").loadAfter("*").build();
        var dependent = builder("dependent", "1.0.0").loadAfter("late").build();

        var result = resolver.resolve(List.of(dependent, late));

        assertThat(result).extracting(Metadata::id).containsExactly("late", "dependent");
    }

    @Test
    @DisplayName("Should honor an explicit loadBefore rule over a wildcard loadAfter")
    void shouldHonorExplicitLoadBeforeOverWildcardLoadAfter() {
        var resolver = new DependencyResolver(List.of());
        var late = builder("late", "1.0.0")
                .loadAfter("*")
                .loadBefore("explicit-before")
                .build();
        var other = meta("explicit-before", "1.0.0");

        var result = resolver.resolve(List.of(other, late));

        assertThat(result).extracting(Metadata::id).containsExactly("late", "explicit-before");
    }

    @Test
    @DisplayName("Should avoid a cycle when two plugins both declare loadAfter wildcard")
    void shouldDisambiguateMutualWildcardLoadAfter() {
        var resolver = new DependencyResolver(List.of());
        var first = builder("a-late", "1.0.0").loadAfter("*").build();
        var second = builder("b-late", "1.0.0").loadAfter("*").build();
        var regular = meta("m-regular", "1.0.0");

        var result = resolver.resolve(List.of(second, regular, first));

        assertThat(result).extracting(Metadata::id).containsExactly("m-regular", "b-late", "a-late");
    }

    @Test
    @DisplayName("Should report only circular nodes when a cycle coexists with independent plugins")
    void shouldReportOnlyCircularNodes() {
        var resolver = new DependencyResolver(List.of());
        var a = withDependencies("a", "1.0.0", Map.of("b", ">=1.0.0"));
        var b = withDependencies("b", "1.0.0", Map.of("a", ">=1.0.0"));
        var independent = meta("independent", "1.0.0");

        assertThatThrownBy(() -> resolver.resolve(List.of(a, b, independent)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Cyclic plugin dependency or load order detected. Circular nodes: a, b");
    }

    @Test
    @DisplayName("Should resolve an empty candidate list to an empty load order")
    void shouldResolveEmptyCandidates() {
        var resolver = new DependencyResolver(List.of());

        assertThat(resolver.resolve(List.of())).isEmpty();
    }

    @Test
    @DisplayName("Should omit core descriptors when only core candidates are provided")
    void shouldOmitCoreOnlyCandidates() {
        Metadata coreLoader = meta(Constants.LOADER_ID, "1.0.0");
        Metadata coreGame = meta("core-game", "1.0.0");
        var resolver = new DependencyResolver(List.of(coreLoader, coreGame));

        var result = resolver.resolve(List.of(coreLoader, coreGame));

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should allow a dependency on a reserved game provider when its core descriptor is registered")
    void shouldAllowDependencyOnRegisteredCoreGameProvider() {
        Metadata coreGame = meta("core-game", "1.0.0");
        Metadata userPlugin = withDependencies(
                "user-plugin",
                "1.0.0",
                Map.of("core-game", ">=1.0.0")
        );
        var resolver = new DependencyResolver(List.of(coreGame));

        var result = resolver.resolve(List.of(coreGame, userPlugin));

        assertThat(result).extracting(Metadata::id).containsExactly("user-plugin");
    }

    private static Metadata meta(String id, String version) {
        return builder(id, version).build();
    }

    private static Metadata withDependencies(String id, String version, Map<String, String> dependencies) {
        return builder(id, version).dependencies(dependencies).build();
    }

    private static Metadata.Builder builder(String id, String version) {
        return new Metadata.Builder()
                .schema(SCHEMA)
                .id(id)
                .name(id + " plugin")
                .version(version)
                .environment(Environment.BOTH);
    }
}
