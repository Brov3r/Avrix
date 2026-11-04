package com.avrix.plugins;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.avrix.core.Environment;
import com.avrix.core.Metadata;
import com.avrix.utils.Constants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link DependencyResolver} validation and deterministic ordering.
 */
@DisplayName("DependencyResolver Tests")
class DependencyResolverTest {

    private static final int SCHEMA = Constants.METADATA_SCHEMA;

    @Test
    @DisplayName("Should order independent plugins deterministically by identifier")
    void shouldOrderIndependentPluginsDeterministically() {
        var resolver = new DependencyResolver(List.of());

        var result = resolver.resolve(List.of(
                meta("plugin-z", "1.0.0"),
                meta("plugin-a", "1.0.0"),
                meta("plugin-m", "1.0.0")
        ));

        assertThat(result).extracting(Metadata::id)
                .containsExactly("plugin-a", "plugin-m", "plugin-z");
    }

    @Test
    @DisplayName("Should reject duplicate plugin identifiers")
    void shouldRejectDuplicateIdentifiers() {
        var resolver = new DependencyResolver(List.of());

        assertThatThrownBy(() -> resolver.resolve(List.of(
                meta("duplicate", "1.0.0"),
                meta("duplicate", "2.0.0")
        )))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Duplicate plugin identifier [duplicate]");
    }

    @Test
    @DisplayName("Should reject uppercase identifiers at the metadata layer")
    void shouldRejectUppercaseIdentifiers() {
        assertThatThrownBy(() -> meta("Plugin", "1.0.0"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must match identifier pattern");
    }

    @Test
    @DisplayName("Should warn and continue when a declared conflict is installed in lenient mode")
    void shouldWarnOnConflictInLenientMode() {
        var pluginA = builder("plugin-a", "1.0.0").conflicts("plugin-b").build();
        var pluginB = meta("plugin-b", "1.0.0");

        Logger logger = (Logger) LoggerFactory.getLogger(DependencyResolver.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            var result = new DependencyResolver(List.of()).resolve(List.of(pluginA, pluginB));
            assertThat(result).extracting(Metadata::id).containsExactlyInAnyOrder("plugin-a", "plugin-b");
        } finally {
            logger.detachAppender(appender);
        }

        assertThat(appender.list).anySatisfy(event -> assertThat(event.getFormattedMessage())
                .contains("Plugin [plugin-a] conflicts with installed plugin [plugin-b]"));
    }

    @Test
    @DisplayName("Should reject a declared conflict in strict mode")
    void shouldRejectConflictInStrictMode() {
        var pluginA = builder("plugin-a", "1.0.0").conflicts("plugin-b").build();
        var pluginB = meta("plugin-b", "1.0.0");

        assertThatThrownBy(() -> new DependencyResolver(List.of(), true).resolve(List.of(pluginA, pluginB)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Plugin [plugin-a] conflicts with installed plugin [plugin-b]");
    }

    @Test
    @DisplayName("Should reject uppercase conflict references at the metadata layer")
    void shouldRejectUppercaseConflictReferences() {
        assertThatThrownBy(() -> builder("plugin-a", "1.0.0").conflicts("PLUGIN-B").build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must match identifier pattern");
    }

    @Test
    @DisplayName("Should ignore conflicts with plugins that are not installed")
    void shouldIgnoreConflictsWithMissingPlugins() {
        var pluginA = builder("plugin-a", "1.0.0").conflicts("missing-plugin").build();
        var pluginB = meta("plugin-b", "1.0.0");

        var result = new DependencyResolver(List.of(), true).resolve(List.of(pluginA, pluginB));

        assertThat(result).extracting(Metadata::id).containsExactlyInAnyOrder("plugin-a", "plugin-b");
    }

    @Test
    @DisplayName("Should reject a self-referencing dependency with a dedicated message")
    void shouldRejectSelfDependency() {
        var resolver = new DependencyResolver(List.of());
        var plugin = withDependencies("self", "1.0.0", Map.of("self", ">=1.0.0"));

        assertThatThrownBy(() -> resolver.resolve(List.of(plugin)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Plugin [self] cannot depend on itself");
    }

    @Test
    @DisplayName("Should reject uppercase dependency and load-order references at the metadata layer")
    void shouldRejectUppercaseReferences() {
        assertThatThrownBy(() -> withDependencies("consumer", "1.0.0", Map.of("MyPlugin", ">=1.0.0")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must match identifier pattern");

        assertThatThrownBy(() -> builder("order-consumer", "1.0.0").loadAfter("Plugin-B").build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must match identifier pattern");
    }

    @Test
    @DisplayName("Should reject self-referencing soft load ordering")
    void shouldRejectSelfSoftOrdering() {
        var resolver = new DependencyResolver(List.of());

        assertThatThrownBy(() -> resolver.resolve(List.of(builder("self", "1.0.0").loadAfter("self").build())))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Plugin [self] cannot declare loadAfter itself");

        assertThatThrownBy(() -> resolver.resolve(List.of(builder("self", "1.0.0").loadBefore("self").build())))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Plugin [self] cannot declare loadBefore itself");
    }

    @Test
    @DisplayName("Should reject the reserved loader identifier when no core descriptor is registered")
    void shouldRejectReservedIdentifiersWithoutCoreRegistration() {
        var resolver = new DependencyResolver(List.of());

        assertThatThrownBy(() -> resolver.resolve(List.of(meta(Constants.LOADER_ID, "1.0.0"))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("[%s] is reserved for a core component".formatted(Constants.LOADER_ID));
    }

    @Test
    @DisplayName("Should accept and omit reserved identifiers when their core descriptors are registered")
    void shouldAcceptReservedIdentifiersWhenCoreDescriptorIsRegistered() {
        Metadata coreLoader = meta(Constants.LOADER_ID, "1.0.0");
        Metadata coreGame = meta("core-game", "1.0.0");
        Metadata userPlugin = meta("user-plugin", "1.0.0");
        var resolver = new DependencyResolver(List.of(coreLoader, coreGame));

        var result = resolver.resolve(List.of(coreLoader, coreGame, userPlugin));

        assertThat(result).extracting(Metadata::id).containsExactly("user-plugin");
    }

    @Test
    @DisplayName("Should reject a dependency that is not part of the candidate set")
    void shouldRejectMissingDependency() {
        var resolver = new DependencyResolver(List.of());
        var orphan = withDependencies("orphan", "1.0.0", Map.of("missing", ">=1.0.0"));

        assertThatThrownBy(() -> resolver.resolve(List.of(orphan)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Plugin [orphan] depends on missing component [missing]");
    }

    @Test
    @DisplayName("Should reject an invalid dependency constraint at the metadata layer")
    void shouldRejectInvalidConstraintBeforeResolution() {
        assertThatThrownBy(() -> withDependencies("consumer", "1.0.0", Map.of("missing", "not-a-valid-range")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Dependency [missing] has invalid version constraint");
    }

    @Test
    @DisplayName("Should reject malformed versions at the metadata layer and report constraint mismatches")
    void shouldRejectMalformedVersionsAndDistinguishMismatch() {
        assertThatThrownBy(() -> meta("provider", "not-a-semver"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid plugin version");

        var resolver = new DependencyResolver(List.of());
        var newerProvider = meta("provider", "3.0.0");
        var incompatibleConsumer = withDependencies("consumer", "1.0.0", Map.of("provider", "^2.1.0"));

        assertThatThrownBy(() -> resolver.resolve(List.of(incompatibleConsumer, newerProvider)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Plugin [consumer] requires [provider] matching [^2.1.0], but found version [3.0.0]");
    }

    @Test
    @DisplayName("Should reject an invalid version constraint while building metadata")
    void shouldRejectInvalidVersionConstraint() {
        assertThatThrownBy(() -> withDependencies("consumer", "1.0.0", Map.of("provider", "not-a-valid-range")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Dependency [provider] has invalid version constraint");
    }

    @Test
    @DisplayName("Should detect a transitive cycle and report the sorted circular nodes")
    void shouldDetectCycles() {
        var resolver = new DependencyResolver(List.of());

        var a = withDependencies("a", "1.0.0", Map.of("b", ">=1.0.0"));
        var b = withDependencies("b", "1.0.0", Map.of("c", ">=1.0.0"));
        var c = withDependencies("c", "1.0.0", Map.of("a", ">=1.0.0"));

        assertThatThrownBy(() -> resolver.resolve(List.of(a, b, c)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Cyclic plugin dependency or load order detected. Circular nodes: a, b, c");
    }

    @Test
    @DisplayName("Should warn about unknown soft load-order targets")
    void shouldWarnAboutUnknownSoftLoadOrderTargets() {
        var plugin = builder("plugin", "1.0.0").loadBefore("missing-target").build();
        Logger logger = (Logger) LoggerFactory.getLogger(DependencyResolver.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            assertThat(new DependencyResolver(List.of()).resolve(List.of(plugin)))
                    .extracting(Metadata::id)
                    .containsExactly("plugin");
        } finally {
            logger.detachAppender(appender);
        }

        assertThat(appender.list).anySatisfy(event -> assertThat(event.getFormattedMessage())
                .contains("Plugin [plugin] declares loadBefore for unknown plugin [missing-target]"));
    }

    @Test
    @DisplayName("Should reject contradictory wildcard load-order declarations")
    void shouldRejectContradictoryWildcardLoadOrder() {
        var plugin = builder("plugin", "1.0.0").loadBefore("*").loadAfter("*").build();

        assertThatThrownBy(() -> new DependencyResolver(List.of()).resolve(List.of(plugin)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Plugin [plugin] cannot declare both loadBefore and loadAfter wildcards");
    }

    @Test
    @DisplayName("Should place wildcard loadBefore first and wildcard loadAfter last")
    void shouldHonorWildcardSoftOrdering() {
        var resolver = new DependencyResolver(List.of());

        var early = builder("early", "1.0.0").loadBefore("*").build();
        var late = builder("late", "1.0.0").loadAfter("*").build();
        var regularA = meta("aaa", "1.0.0");
        var regularZ = meta("zzz", "1.0.0");

        var result = resolver.resolve(List.of(late, regularZ, early, regularA));

        assertThat(result.getFirst().id()).isEqualTo("early");
        assertThat(result.getLast().id()).isEqualTo("late");
        assertThat(result.subList(1, 3)).extracting(Metadata::id)
                .containsExactly("aaa", "zzz");
    }

    @Test
    @DisplayName("Should honor direct loadBefore and loadAfter constraints")
    void shouldHonorDirectSoftOrdering() {
        var resolver = new DependencyResolver(List.of());

        var a = builder("a", "1.0.0").loadAfter("b").build();
        var b = builder("b", "1.0.0").loadBefore("c").build();
        var c = meta("c", "1.0.0");

        var result = resolver.resolve(List.of(c, a, b));

        assertThat(result).extracting(Metadata::id).containsExactly("b", "a", "c");
    }

    @Test
    @DisplayName("Should reject null inputs")
    void shouldRejectNullInputs() {
        var resolver = new DependencyResolver(List.of());

        assertThatThrownBy(() -> resolver.resolve(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Candidates list cannot be null");

        assertThatThrownBy(() -> new DependencyResolver(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Core plugins cannot be null");
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
