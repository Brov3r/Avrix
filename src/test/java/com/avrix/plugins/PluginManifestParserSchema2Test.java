package com.avrix.plugins;

import com.avrix.core.Environment;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for schema-2 side-specific entrypoint and mixin declarations.
 */
@DisplayName("PluginManifestParser Schema 2 Tests")
class PluginManifestParserSchema2Test {

    private final PluginManifestParser parser = new PluginManifestParser();

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("Should parse all entrypoint and mixin sides from a schema-2 manifest")
    void shouldParseAllSides() throws IOException {
        ConfigurationNode node = load("""
                schema: 2
                id: sides-plugin
                name: Sides Plugin
                version: 1.0.0
                environment: both
                entrypoints:
                  main:
                    - com.example.MainA
                    - com.example.MainB
                  client:
                    - com.example.ClientMain
                    - com.example.MainA
                  server:
                    - com.example.ServerMain
                mixins:
                  common:
                    - com.example.mixins.Common
                  client:
                    - com.example.mixins.Client
                    - com.example.mixins.Common
                  server:
                    - com.example.mixins.Server
                """);

        var metadata = parser.fromNode(node);

        assertThat(metadata.schema()).isEqualTo(2);
        assertThat(metadata.environment()).isEqualTo(Environment.BOTH);
        assertThat(metadata.entrypoint()).isEqualTo("com.example.MainA");
        assertThat(metadata.mainEntrypoints()).containsExactly("com.example.MainA", "com.example.MainB");
        assertThat(metadata.clientEntrypoints())
                .containsExactly("com.example.ClientMain", "com.example.MainA");
        assertThat(metadata.serverEntrypoints()).containsExactly("com.example.ServerMain");
        assertThat(metadata.mixins()).containsExactly("com.example.mixins.Common");
        assertThat(metadata.clientMixins())
                .containsExactly("com.example.mixins.Client", "com.example.mixins.Common");
        assertThat(metadata.serverMixins()).containsExactly("com.example.mixins.Server");

        assertThat(metadata.entrypointsFor(Environment.CLIENT))
                .containsExactly("com.example.MainA", "com.example.MainB", "com.example.ClientMain");
        assertThat(metadata.entrypointsFor(Environment.SERVER))
                .containsExactly("com.example.MainA", "com.example.MainB", "com.example.ServerMain");
        assertThat(metadata.entrypointsFor(Environment.BOTH))
                .containsExactly("com.example.MainA", "com.example.MainB");
        assertThat(metadata.mixinsFor(Environment.CLIENT))
                .containsExactly("com.example.mixins.Common", "com.example.mixins.Client");
        assertThat(metadata.mixinsFor(Environment.SERVER))
                .containsExactly("com.example.mixins.Common", "com.example.mixins.Server");
        assertThat(metadata.mixinsFor(Environment.BOTH)).containsExactly("com.example.mixins.Common");
    }

    @Test
    @DisplayName("Should accept schema-2 singular entrypoint and flat mixins as main and common shorthands")
    void shouldAcceptSingularAndFlatShorthands() throws IOException {
        ConfigurationNode node = load("""
                schema: 2
                id: shorthand-plugin
                name: Shorthand Plugin
                version: 1.0.0
                entrypoint: com.example.Main
                mixins:
                  - com.example.mixins.Common
                """);

        var metadata = parser.fromNode(node);

        assertThat(metadata.mainEntrypoints()).containsExactly("com.example.Main");
        assertThat(metadata.clientEntrypoints()).isEmpty();
        assertThat(metadata.serverEntrypoints()).isEmpty();
        assertThat(metadata.mixins()).containsExactly("com.example.mixins.Common");
        assertThat(metadata.clientMixins()).isEmpty();
        assertThat(metadata.serverMixins()).isEmpty();
        assertThat(metadata.entrypointsFor(Environment.CLIENT)).containsExactly("com.example.Main");
        assertThat(metadata.mixinsFor(Environment.SERVER)).containsExactly("com.example.mixins.Common");
    }

    @Test
    @DisplayName("Should allow omitted schema-2 side sections")
    void shouldAllowOmittedSections() throws IOException {
        ConfigurationNode node = load("""
                schema: 2
                id: minimal-schema2-plugin
                name: Minimal Schema 2 Plugin
                version: 1.0.0
                """);

        var metadata = parser.fromNode(node);

        assertThat(metadata.schema()).isEqualTo(2);
        assertThat(metadata.mainEntrypoints()).isEmpty();
        assertThat(metadata.entrypoint()).isEmpty();
        assertThat(metadata.clientEntrypoints()).isEmpty();
        assertThat(metadata.serverEntrypoints()).isEmpty();
        assertThat(metadata.mixins()).isEmpty();
        assertThat(metadata.clientMixins()).isEmpty();
        assertThat(metadata.serverMixins()).isEmpty();
        assertThat(metadata.entrypointsFor(Environment.CLIENT)).isEmpty();
        assertThat(metadata.mixinsFor(Environment.SERVER)).isEmpty();
    }

    @Test
    @DisplayName("Should reject combining entrypoint with entrypoints")
    void shouldRejectMixedEntrypointForms() throws IOException {
        ConfigurationNode node = load("""
                schema: 2
                id: mixed-entrypoint-plugin
                name: Mixed Entrypoint Plugin
                version: 1.0.0
                entrypoint: com.example.Main
                entrypoints:
                  client:
                    - com.example.Client
                """);

        assertThatThrownBy(() -> parser.fromNode(node))
                .isInstanceOf(SerializationException.class)
                .hasMessageContaining("[entrypoint] and [entrypoints] cannot be combined");
    }

    @Test
    @DisplayName("Should reject unknown entrypoint sides")
    void shouldRejectUnknownEntrypointSides() throws IOException {
        ConfigurationNode node = load("""
                schema: 2
                id: unknown-entrypoint-side-plugin
                name: Unknown Entrypoint Side Plugin
                version: 1.0.0
                entrypoints:
                  desktop:
                    - com.example.Desktop
                """);

        assertThatThrownBy(() -> parser.fromNode(node))
                .isInstanceOf(SerializationException.class)
                .hasMessageContaining("Unknown key [desktop] in metadata section [entrypoints]");
    }

    @Test
    @DisplayName("Should reject unknown mixin sides")
    void shouldRejectUnknownMixinSides() throws IOException {
        ConfigurationNode node = load("""
                schema: 2
                id: unknown-mixin-side-plugin
                name: Unknown Mixin Side Plugin
                version: 1.0.0
                mixins:
                  desktop:
                    - com.example.mixins.Desktop
                """);

        assertThatThrownBy(() -> parser.fromNode(node))
                .isInstanceOf(SerializationException.class)
                .hasMessageContaining("Unknown key [desktop] in metadata section [mixins]");
    }

    @Test
    @DisplayName("Should reject an entrypoints section that is not a map")
    void shouldRejectNonMapEntrypoints() throws IOException {
        ConfigurationNode node = load("""
                schema: 2
                id: list-entrypoints-plugin
                name: List Entrypoints Plugin
                version: 1.0.0
                entrypoints:
                  - com.example.Main
                """);

        assertThatThrownBy(() -> parser.fromNode(node))
                .isInstanceOf(SerializationException.class)
                .hasMessageContaining("Metadata key [entrypoints] must be a map");
    }

    @Test
    @DisplayName("Should reject a side that is not a list of strings")
    void shouldRejectScalarSide() throws IOException {
        ConfigurationNode node = load("""
                schema: 2
                id: scalar-side-plugin
                name: Scalar Side Plugin
                version: 1.0.0
                entrypoints:
                  client: com.example.Client
                """);

        assertThatThrownBy(() -> parser.fromNode(node))
                .isInstanceOf(SerializationException.class)
                .hasMessageContaining("Metadata key [entrypoints.client] must be a list of strings");
    }

    @Test
    @DisplayName("Should reject an invalid class name declared on a side")
    void shouldRejectInvalidSideClassName() throws IOException {
        ConfigurationNode node = load("""
                schema: 2
                id: invalid-side-class-plugin
                name: Invalid Side Class Plugin
                version: 1.0.0
                entrypoints:
                  client:
                    - invalid class name
                """);

        assertThatThrownBy(() -> parser.fromNode(node))
                .isInstanceOf(SerializationException.class)
                .hasMessageContaining("client entrypoints")
                .hasMessageContaining("not a valid Java class name");
    }

    @Test
    @DisplayName("Should not load declared side classes while parsing")
    void shouldNotLoadSideClasses() throws IOException {
        ConfigurationNode node = load("""
                schema: 2
                id: missing-class-plugin
                name: Missing Class Plugin
                version: 1.0.0
                entrypoints:
                  client:
                    - not.present.MissingClient
                mixins:
                  server:
                    - not.present.mixins.MissingServerMixin
                """);

        var metadata = parser.fromNode(node);

        assertThat(metadata.entrypointsFor(Environment.CLIENT))
                .containsExactly("not.present.MissingClient");
        assertThat(metadata.mixinsFor(Environment.SERVER))
                .containsExactly("not.present.mixins.MissingServerMixin");
    }

    @Test
    @DisplayName("Should remove duplicates and blank entries within each declared side")
    void shouldDeduplicateSides() throws IOException {
        ConfigurationNode node = load("""
                schema: 2
                id: deduplicating-plugin
                name: Deduplicating Plugin
                version: 1.0.0
                entrypoints:
                  main:
                    - com.example.Main
                    - com.example.Main
                    - ""
                  client:
                    - com.example.Main
                    - com.example.Client
                    - com.example.Client
                mixins:
                  common:
                    - com.example.mixins.Common
                    - com.example.mixins.Common
                  client:
                    - com.example.mixins.Common
                    - com.example.mixins.Client
                """);

        var metadata = parser.fromNode(node);

        assertThat(metadata.mainEntrypoints()).containsExactly("com.example.Main");
        assertThat(metadata.clientEntrypoints())
                .containsExactly("com.example.Main", "com.example.Client");
        assertThat(metadata.entrypointsFor(Environment.CLIENT))
                .containsExactly("com.example.Main", "com.example.Client");
        assertThat(metadata.mixins()).containsExactly("com.example.mixins.Common");
        assertThat(metadata.clientMixins())
                .containsExactly("com.example.mixins.Common", "com.example.mixins.Client");
        assertThat(metadata.mixinsFor(Environment.CLIENT))
                .containsExactly("com.example.mixins.Common", "com.example.mixins.Client");
    }

    @Test
    @DisplayName("Should reject schema-2 entrypoints syntax in schema 1")
    void shouldRejectEntrypointsInSchema1() throws IOException {
        ConfigurationNode node = load("""
                schema: 1
                id: schema1-entrypoints-plugin
                name: Schema 1 Entrypoints Plugin
                version: 1.0.0
                entrypoints:
                  main:
                    - com.example.Main
                """);

        assertThatThrownBy(() -> parser.fromNode(node))
                .isInstanceOf(SerializationException.class)
                .hasMessageContaining("Metadata key [entrypoints] requires metadata schema 2");
    }

    @Test
    @DisplayName("Should reject schema-2 mixin sections in schema 1")
    void shouldRejectMixinSectionsInSchema1() throws IOException {
        ConfigurationNode node = load("""
                schema: 1
                id: schema1-mixin-sections-plugin
                name: Schema 1 Mixin Sections Plugin
                version: 1.0.0
                mixins:
                  common:
                    - com.example.mixins.Common
                """);

        assertThatThrownBy(() -> parser.fromNode(node))
                .isInstanceOf(SerializationException.class)
                .hasMessageContaining("Metadata key [mixins] must be a list of strings in metadata schema 1");
    }

    @Test
    @DisplayName("Should parse schema-2 sides from a YAML file on disk")
    void shouldParseSchema2FromYamlFile() throws IOException {
        Path yamlPath = tempDir.resolve("schema2-plugin.yml");
        Files.writeString(yamlPath, """
                schema: 2
                id: file-schema2-plugin
                name: File Schema 2 Plugin
                version: 1.0.0
                entrypoints:
                  main:
                    - com.example.Main
                  server:
                    - com.example.Server
                mixins:
                  client:
                    - com.example.mixins.Client
                """, StandardCharsets.UTF_8);

        var metadata = parser.fromYaml(yamlPath);

        assertThat(metadata.entrypointsFor(Environment.SERVER))
                .containsExactly("com.example.Main", "com.example.Server");
        assertThat(metadata.entrypointsFor(Environment.CLIENT)).containsExactly("com.example.Main");
        assertThat(metadata.mixinsFor(Environment.CLIENT)).containsExactly("com.example.mixins.Client");
        assertThat(metadata.mixinsFor(Environment.SERVER)).isEmpty();
    }

    private ConfigurationNode load(String yaml) throws IOException {
        YamlConfigurationLoader loader = YamlConfigurationLoader.builder()
                .source(() -> new BufferedReader(new StringReader(yaml)))
                .build();
        return loader.load();
    }
}
