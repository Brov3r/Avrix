package com.avrix.plugins;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.avrix.core.Environment;
import com.avrix.utils.Constants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Validation tests for {@link PluginManifestParser}.
 */
@DisplayName("PluginManifestParser Validation Tests")
class PluginManifestParserValidationTest {

    private final PluginManifestParser lenientParser = new PluginManifestParser();

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("Should warn about unknown top-level keys in every validation mode")
    void shouldWarnAboutUnknownTopLevelKeys() throws IOException {
        ConfigurationNode node = load("""
                schema: 1
                id: valid-plugin
                name: Valid Plugin
                version: "1.0.0"
                entryPoint: com.example.Main
                """);

        assertThat(lenientParser.fromNode(node).id()).isEqualTo("valid-plugin");
        assertThat(lenientParser.fromNode(node).id()).isEqualTo("valid-plugin");
    }

    @Test
    @DisplayName("Should require schema in every validation mode")
    void shouldRequireSchema() throws IOException {
        ConfigurationNode node = load("""
                id: no-schema
                name: No Schema
                version: "1.0.0"
                """);

        assertThatThrownBy(() -> lenientParser.fromNode(node))
                .isInstanceOf(SerializationException.class)
                .hasMessageContaining("Missing required metadata key [schema]");
        assertThatThrownBy(() -> lenientParser.fromNode(node))
                .isInstanceOf(SerializationException.class)
                .hasMessageContaining("Missing required metadata key [schema]");
    }

    @Test
    @DisplayName("Should reject a non-integer schema value")
    void shouldRejectNonIntegerSchema() throws IOException {
        ConfigurationNode node = load("""
                schema: abc
                id: schema-plugin
                name: Schema Plugin
                version: "1.0.0"
                """);

        assertThatThrownBy(() -> lenientParser.fromNode(node))
                .isInstanceOf(SerializationException.class)
                .hasMessageContaining("Metadata key [schema] must be an integer");
    }

    @Test
    @DisplayName("Should reject an unsupported metadata schema version")
    void shouldRejectUnsupportedSchemaVersion() throws IOException {
        ConfigurationNode node = load("""
                schema: 2
                id: future-plugin
                name: Future Plugin
                version: "1.0.0"
                """);

        assertThatThrownBy(() -> lenientParser.fromNode(node))
                .isInstanceOf(SerializationException.class)
                .hasMessageContaining("Unsupported metadata schema [2]")
                .hasMessageContaining("supported: " + Constants.METADATA_SCHEMA);
    }

    @Test
    @DisplayName("Should default unknown environments to BOTH in every validation mode")
    void shouldValidateEnvironment() throws IOException {
        ConfigurationNode node = load("""
                schema: 1
                id: environment-plugin
                name: Environment Plugin
                version: "1.0.0"
                environment: cliet
                """);

        assertThat(lenientParser.fromNode(node).environment()).isEqualTo(Environment.BOTH);
        assertThat(lenientParser.fromNode(node).environment()).isEqualTo(Environment.BOTH);
    }

    @Test
    @DisplayName("Should reject dependencies declared as a list")
    void shouldRejectNonMapDependencies() throws IOException {
        ConfigurationNode node = load("""
                schema: 1
                id: dependency-plugin
                name: Dependency Plugin
                version: "1.0.0"
                dependencies:
                  - core-lib
                """);

        assertThatThrownBy(() -> lenientParser.fromNode(node))
                .isInstanceOf(SerializationException.class)
                .hasMessageContaining("Metadata key [dependencies] must be a map");
    }

    @Test
    @DisplayName("Should reject a dependency with a blank version constraint")
    void shouldRejectBlankDependencyConstraint() throws IOException {
        ConfigurationNode node = load("""
                schema: 1
                id: dependency-plugin
                name: Dependency Plugin
                version: "1.0.0"
                dependencies:
                  core-lib: ""
                """);

        assertThatThrownBy(() -> lenientParser.fromNode(node))
                .isInstanceOf(SerializationException.class)
                .hasMessageContaining("Dependency [core-lib] has a blank version constraint");
    }

    @Test
    @DisplayName("Should accept an explicit wildcard dependency constraint")
    void shouldAcceptExplicitWildcardDependencyConstraint() throws IOException {
        ConfigurationNode node = load("""
                schema: 1
                id: dependency-plugin
                name: Dependency Plugin
                version: "1.0.0"
                dependencies:
                  core-lib: "*"
                """);

        assertThat(lenientParser.fromNode(node).dependencies()).containsEntry("core-lib", "*");
    }

    @Test
    @DisplayName("Should reject an invalid dependency version constraint")
    void shouldRejectInvalidDependencyConstraint() throws IOException {
        ConfigurationNode node = load("""
                schema: 1
                id: dependency-plugin
                name: Dependency Plugin
                version: "1.0.0"
                dependencies:
                  core-lib: "not-a-range"
                """);

        assertThatThrownBy(() -> lenientParser.fromNode(node))
                .isInstanceOf(SerializationException.class)
                .hasMessageContaining("Dependency [core-lib]")
                .hasMessageContaining("invalid version constraint [not-a-range]");
    }

    @Test
    @DisplayName("Should validate identifiers in load order lists")
    void shouldValidateLoadOrderIdentifiers() throws IOException {
        ConfigurationNode node = load("""
                schema: 1
                id: order-plugin
                name: Order Plugin
                version: "1.0.0"
                loadBefore:
                  - "bad id"
                """);

        assertThatThrownBy(() -> lenientParser.fromNode(node))
                .isInstanceOf(SerializationException.class)
                .hasMessageContaining("Plugin 'loadBefore'")
                .hasMessageContaining("cannot contain whitespace");
    }

    @Test
    @DisplayName("Should deduplicate load order identifiers in every validation mode")
    void shouldDeduplicateLoadOrderIdentifiers() throws IOException {
        ConfigurationNode node = load("""
                schema: 1
                id: order-plugin
                name: Order Plugin
                version: "1.0.0"
                loadBefore:
                  - plugin-a
                  - plugin-a
                """);

        assertThat(lenientParser.fromNode(node).loadBefore()).containsExactly("plugin-a");
        assertThat(lenientParser.fromNode(node).loadBefore()).containsExactly("plugin-a");
    }

    @Test
    @DisplayName("Should reject an invalid entrypoint class name")
    void shouldRejectInvalidEntrypoint() throws IOException {
        ConfigurationNode node = load("""
                schema: 1
                id: entrypoint-plugin
                name: Entrypoint Plugin
                version: "1.0.0"
                entrypoint: com.example..Main
                """);

        assertThatThrownBy(() -> lenientParser.fromNode(node))
                .isInstanceOf(SerializationException.class)
                .hasMessageContaining("Plugin 'entrypoint'")
                .hasMessageContaining("not a valid Java class name");
    }

    @Test
    @DisplayName("Should accept nested Java class names for entrypoints and mixins")
    void shouldAcceptNestedJavaClassNames() throws IOException {
        ConfigurationNode node = load("""
                schema: 1
                id: nested-plugin
                name: Nested Plugin
                version: "1.0.0"
                entrypoint: com.example.Outer$Inner
                mixins:
                  - com.example.mixins.Outer$Mixin
                """);

        var metadata = lenientParser.fromNode(node);

        assertThat(metadata.entrypoint()).isEqualTo("com.example.Outer$Inner");
        assertThat(metadata.mixins()).containsExactly("com.example.mixins.Outer$Mixin");
    }

    @Test
    @DisplayName("Should reject an invalid mixin class name")
    void shouldRejectInvalidMixin() throws IOException {
        ConfigurationNode node = load("""
                schema: 1
                id: mixin-plugin
                name: Mixin Plugin
                version: "1.0.0"
                mixins:
                  - invalid mixin name
                """);

        assertThatThrownBy(() -> lenientParser.fromNode(node))
                .isInstanceOf(SerializationException.class)
                .hasMessageContaining("Plugin mixin")
                .hasMessageContaining("not a valid Java class name");
    }

    @Test
    @DisplayName("Should parse declared conflicts")
    void shouldParseConflicts() throws IOException {
        ConfigurationNode node = load("""
                schema: 1
                id: conflict-plugin
                name: Conflict Plugin
                version: "1.0.0"
                conflicts:
                  - legacy-plugin
                  - old-api
                """);

        assertThat(lenientParser.fromNode(node).conflicts())
                .containsExactly("legacy-plugin", "old-api");
    }

    @Test
    @DisplayName("Should reject invalid conflict identifiers")
    void shouldRejectInvalidConflictIdentifiers() throws IOException {
        ConfigurationNode node = load("""
                schema: 1
                id: conflict-plugin
                name: Conflict Plugin
                version: "1.0.0"
                conflicts:
                  - LegacyPlugin
                """);

        assertThatThrownBy(() -> lenientParser.fromNode(node))
                .isInstanceOf(SerializationException.class)
                .hasMessageContaining("Plugin 'conflicts'")
                .hasMessageContaining("must match identifier pattern");
    }

    @Test
    @DisplayName("Should report missing required metadata fields")
    void shouldRejectMissingRequiredFields() throws IOException {
        ConfigurationNode node = load("""
                schema: 1
                name: Missing Identifier
                version: "1.0.0"
                """);

        assertThatThrownBy(() -> lenientParser.fromNode(node))
                .isInstanceOf(SerializationException.class)
                .hasMessageContaining("Invalid metadata")
                .hasMessageContaining("Plugin 'id' cannot be null");
    }

    @Test
    @DisplayName("Should reject an invalid plugin version")
    void shouldRejectInvalidPluginVersion() throws IOException {
        ConfigurationNode node = load("""
                schema: 1
                id: version-plugin
                name: Version Plugin
                version: not-a-semver
                """);

        assertThatThrownBy(() -> lenientParser.fromNode(node))
                .isInstanceOf(SerializationException.class)
                .hasMessageContaining("Invalid plugin version [not-a-semver]");
    }

    @Test
    @DisplayName("Should reject whitespace inside identifiers")
    void shouldRejectWhitespaceInsideIdentifiers() throws IOException {
        ConfigurationNode node = load("""
                schema: 1
                id: "bad id"
                name: Bad Identifier
                version: "1.0.0"
                """);

        assertThatThrownBy(() -> lenientParser.fromNode(node))
                .isInstanceOf(SerializationException.class)
                .hasMessageContaining("Plugin 'id' cannot contain whitespace");
    }

    @Test
    @DisplayName("Should reject non-canonical identifiers in every validation mode")
    void shouldValidateIdentifierPattern() throws IOException {
        ConfigurationNode node = load("""
                schema: 1
                id: UppercasePlugin
                name: Uppercase Plugin
                version: "1.0.0"
                """);

        assertThatThrownBy(() -> lenientParser.fromNode(node))
                .isInstanceOf(SerializationException.class)
                .hasMessageContaining("must match identifier pattern");
        assertThatThrownBy(() -> lenientParser.fromNode(node))
                .isInstanceOf(SerializationException.class)
                .hasMessageContaining("must match identifier pattern");
    }

    @Test
    @DisplayName("Should ignore blank list entries in every validation mode")
    void shouldIgnoreBlankListEntries() throws IOException {
        ConfigurationNode node = load("""
                schema: 1
                id: list-plugin
                name: List Plugin
                version: "1.0.0"
                authors:
                  - ""
                """);

        assertThat(lenientParser.fromNode(node).authors()).isEmpty();
        assertThat(lenientParser.fromNode(node).authors()).isEmpty();
    }

    @Test
    @DisplayName("Should reject duplicate YAML keys")
    void shouldRejectDuplicateYamlKeys() throws IOException {
        Path yamlPath = tempDir.resolve("duplicate-metadata.yml");
        Files.writeString(yamlPath, """
                schema: 1
                id: first-plugin
                id: second-plugin
                name: Duplicate Plugin
                version: "1.0.0"
                """, StandardCharsets.UTF_8);

        assertThatThrownBy(() -> lenientParser.fromYaml(yamlPath))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("Invalid metadata in YAML")
                .hasMessageContaining(yamlPath.toString())
                .hasMessageContaining("duplicate key");
    }

    @Test
    @DisplayName("Should include the JAR source in manifest warnings")
    void shouldIncludeJarSourceInWarnings() throws IOException {
        Path jarPath = tempDir.resolve("warning-plugin.jar");
        writeJar(jarPath, """
                schema: 1
                id: warning-plugin
                name: Warning Plugin
                version: "1.0.0"
                typoKey: value
                """);

        Logger logger = (Logger) LoggerFactory.getLogger(PluginManifestParser.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            assertThat(lenientParser.fromJarFile(jarPath, Constants.METADATA_NAME).id())
                    .isEqualTo("warning-plugin");
        } finally {
            logger.detachAppender(appender);
        }

        assertThat(appender.list).anySatisfy(event -> assertThat(event.getFormattedMessage())
                .contains("warning-plugin.jar")
                .contains("Unknown metadata key [typoKey]"));
    }

    @Test
    @DisplayName("Should include the JAR source in non-fatal manifest warnings")
    void shouldIncludeJarSourceInWarningsForBothModes() throws IOException {
        Path jarPath = tempDir.resolve("plugin.jar");
        writeJar(jarPath, """
                schema: 1
                id: jar-plugin
                name: JAR Plugin
                version: "1.0.0"
                typoKey: value
                """);

        Logger logger = (Logger) LoggerFactory.getLogger(PluginManifestParser.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            assertThat(lenientParser.fromJarFile(jarPath, Constants.METADATA_NAME).id())
                    .isEqualTo("jar-plugin");
        } finally {
            logger.detachAppender(appender);
        }

        assertThat(appender.list).anySatisfy(event -> assertThat(event.getFormattedMessage())
                .contains("plugin.jar")
                .contains("Unknown metadata key [typoKey]"));
    }

    @Test
    @DisplayName("Should reject duplicate YAML keys inside a JAR manifest")
    void shouldRejectDuplicateYamlKeysInJar() throws IOException {
        Path jarPath = tempDir.resolve("duplicate-keys.jar");
        writeJar(jarPath, """
                schema: 1
                id: first-plugin
                id: second-plugin
                name: Duplicate Plugin
                version: "1.0.0"
                """);

        assertThatThrownBy(() -> lenientParser.fromJarFile(jarPath, Constants.METADATA_NAME))
                .hasMessageContaining("duplicate key");
    }

    private ConfigurationNode load(String yaml) throws IOException {
        YamlConfigurationLoader loader = YamlConfigurationLoader.builder()
                .source(() -> new BufferedReader(new StringReader(yaml)))
                .build();
        return loader.load();
    }

    private static void writeJar(Path jarPath, String yaml) throws IOException {
        Files.createDirectories(jarPath.getParent());
        try (JarOutputStream jar = new JarOutputStream(Files.newOutputStream(jarPath))) {
            jar.putNextEntry(new JarEntry(Constants.METADATA_NAME));
            jar.write(yaml.getBytes(StandardCharsets.UTF_8));
            jar.closeEntry();
        }
    }
}
