package com.avrix.core;

import com.avrix.provider.GameProvider;
import com.avrix.utils.Constants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit test suite for {@link Metadata} record, builder, and deserialization routines.
 */
@DisplayName("Metadata Unit Tests")
class MetadataTest {

    private static final int SCHEMA = Constants.METADATA_SCHEMA;

    @Nested
    @DisplayName("Constructor & Immutability Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create instance and retain exact values")
        void shouldCreateInstance() {
            var dependencies = Map.of("core-plugin", ">=1.0.0");
            var authors = List.of("Author One", "Author Two");
            var contacts = List.of("dev@example.com");
            var mixins = List.of("mixins.core.class");

            var meta = new Metadata(
                    SCHEMA,
                    "Test Plugin",
                    "A test plugin description",
                    "test-plugin",
                    "1.2.3",
                    Environment.SERVER,
                    authors,
                    "MIT",
                    contacts,
                    dependencies,
                    "com.example.PluginMain",
                    mixins
            );

            assertThat(meta.schema()).isEqualTo(SCHEMA);
            assertThat(meta.name()).isEqualTo("Test Plugin");
            assertThat(meta.description()).isEqualTo("A test plugin description");
            assertThat(meta.id()).isEqualTo("test-plugin");
            assertThat(meta.version()).isEqualTo("1.2.3");
            assertThat(meta.environment()).isEqualTo(Environment.SERVER);
            assertThat(meta.authors()).containsExactly("Author One", "Author Two");
            assertThat(meta.license()).isEqualTo("MIT");
            assertThat(meta.contacts()).containsExactly("dev@example.com");
            assertThat(meta.dependencies()).containsEntry("core-plugin", ">=1.0.0");
            assertThat(meta.entrypoint()).isEqualTo("com.example.PluginMain");
            assertThat(meta.mixins()).containsExactly("mixins.core.class");
        }

        @Test
        @DisplayName("Should return strictly unmodifiable collections")
        void shouldReturnUnmodifiableCollections() {
            var meta = new Metadata(
                    SCHEMA,
                    "Name",
                    "Desc",
                    "plugin-id",
                    "1.0.0",
                    Environment.BOTH,
                    List.of("Author"),
                    "MIT",
                    List.of("contact@example.com"),
                    Map.of("dep", "1.0"),
                    "com.example.Main",
                    List.of("mixin.json")
            );

            assertThatThrownBy(() -> meta.authors().add("Unauthorized"))
                    .isInstanceOf(UnsupportedOperationException.class);
            assertThatThrownBy(() -> meta.contacts().clear())
                    .isInstanceOf(UnsupportedOperationException.class);
            assertThatThrownBy(() -> meta.dependencies().put("key", "val"))
                    .isInstanceOf(UnsupportedOperationException.class);
            assertThatThrownBy(() -> meta.mixins().remove(0))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    @DisplayName("Builder Tests")
    class BuilderTests {

        @Test
        @DisplayName("Should construct valid metadata with minimal required fields and fallback defaults")
        void shouldBuildWithMinimalFields() {
            var meta = new Metadata.Builder()
                    .schema(SCHEMA)
                    .id("minimal-id")
                    .name("Minimal Plugin")
                    .version("0.1.0")
                    .build();

            assertThat(meta.id()).isEqualTo("minimal-id");
            assertThat(meta.name()).isEqualTo("Minimal Plugin");
            assertThat(meta.version()).isEqualTo("0.1.0");
            assertThat(meta.environment()).isEqualTo(Environment.BOTH);
            assertThat(meta.description()).isEmpty();
            assertThat(meta.license()).isEqualTo("UNLICENSED");
            assertThat(meta.entrypoint()).isEmpty();
            assertThat(meta.authors()).isEmpty();
            assertThat(meta.contacts()).isEmpty();
            assertThat(meta.dependencies()).isEmpty();
            assertThat(meta.mixins()).isEmpty();
        }

        @Test
        @DisplayName("Should fail on unsupported metadata schema version")
        void shouldFailOnWrongSchema() {
            assertThatThrownBy(() -> new Metadata.Builder()
                    .schema(999)
                    .id("plugin-id")
                    .name("Plugin Name")
                    .version("1.0.0")
                    .build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Unsupported metadata schema version");
        }

        @Test
        @DisplayName("Should fail on missing or blank required fields")
        void shouldFailOnMissingFields() {
            // Missing ID (null)
            assertThatThrownBy(() -> new Metadata.Builder()
                    .schema(SCHEMA)
                    .name("Name")
                    .version("1.0.0")
                    .build())
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("Plugin 'id' cannot be null");

            // Blank ID
            assertThatThrownBy(() -> new Metadata.Builder()
                    .schema(SCHEMA)
                    .id("   ")
                    .name("Name")
                    .version("1.0.0")
                    .build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Plugin 'id' cannot be blank");

            // Missing Name (null)
            assertThatThrownBy(() -> new Metadata.Builder()
                    .schema(SCHEMA)
                    .id("valid-id")
                    .version("1.0.0")
                    .build())
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("Plugin 'name' cannot be null");

            // Missing Version (null)
            assertThatThrownBy(() -> new Metadata.Builder()
                    .schema(SCHEMA)
                    .id("valid-id")
                    .name("Valid Name")
                    .build())
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("Plugin 'version' cannot be null");

            // Blank Version
            assertThatThrownBy(() -> new Metadata.Builder()
                    .schema(SCHEMA)
                    .id("valid-id")
                    .name("Valid Name")
                    .version("   ")
                    .build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Plugin 'version' cannot be blank");
        }

        @Test
        @DisplayName("Should defensively copy collections passed to builder")
        void shouldDefensivelyCopyCollections() {
            var mutableAuthors = new ArrayList<>(List.of("Original Author"));
            var mutableDependencies = new HashMap<>(Map.of("dep-a", "1.0.0"));

            var builder = new Metadata.Builder()
                    .schema(SCHEMA)
                    .id("defensive-id")
                    .name("Defensive Name")
                    .version("1.0.0")
                    .authors(mutableAuthors)
                    .dependencies(mutableDependencies);

            mutableAuthors.add("Intruder");
            mutableDependencies.put("malicious-dep", "6.6.6");

            var meta = builder.build();

            assertThat(meta.authors()).containsExactly("Original Author");
            assertThat(meta.dependencies()).doesNotContainKey("malicious-dep");
        }

        @Test
        @DisplayName("Should support fluent varargs and single-element addition")
        void shouldSupportConvenienceMethods() {
            var meta = new Metadata.Builder()
                    .schema(SCHEMA)
                    .id("convenience-id")
                    .name("Convenience Name")
                    .version("1.0.0")
                    .authors("Author A", "Author B")
                    .addAuthor("Author C")
                    .contacts("contact1@example.com")
                    .addContact("contact2@example.com")
                    .addDependency("lib-core", ">=2.0.0")
                    .addDependency("lib-ext", "^1.0.0")
                    .mixins("mixin.a.json", "mixin.b.json")
                    .addMixin("mixin.c.json")
                    .build();

            assertThat(meta.authors()).containsExactly("Author A", "Author B", "Author C");
            assertThat(meta.contacts()).containsExactly("contact1@example.com", "contact2@example.com");
            assertThat(meta.dependencies())
                    .containsEntry("lib-core", ">=2.0.0")
                    .containsEntry("lib-ext", "^1.0.0");
            assertThat(meta.mixins()).containsExactly("mixin.a.json", "mixin.b.json", "mixin.c.json");
        }
    }

    @Nested
    @DisplayName("fromNode YAML Deserialization Tests")
    class FromNodeTests {

        private ConfigurationNode loadYamlNode(String yamlContent) throws IOException {
            var loader = YamlConfigurationLoader.builder()
                    .source(() -> new BufferedReader(new StringReader(yamlContent)))
                    .build();
            return loader.load();
        }

        @Test
        @DisplayName("Should parse complete metadata descriptor from YAML structure")
        void shouldParseCompleteNode() throws IOException {
            var yaml = """
                    schema: %d
                    id: parsed-plugin
                    name: Parsed Plugin
                    description: Parsed from YAML
                    version: 2.5.1
                    environment: client
                    entrypoint: com.example.ParsedEntrypoint
                    license: Apache-2.0
                    authors:
                      - Dev One
                      - Dev Two
                    contacts:
                      - https://example.com
                      - mail@example.com
                    dependencies:
                      core-lib: ">=1.0.0"
                      optional-dep: "^2.3"
                    mixins:
                      - mixins.parsed.class
                    """.formatted(SCHEMA);

            var node = loadYamlNode(yaml);
            var meta = Metadata.fromNode(node);

            assertThat(meta.schema()).isEqualTo(SCHEMA);
            assertThat(meta.id()).isEqualTo("parsed-plugin");
            assertThat(meta.name()).isEqualTo("Parsed Plugin");
            assertThat(meta.description()).isEqualTo("Parsed from YAML");
            assertThat(meta.version()).isEqualTo("2.5.1");
            assertThat(meta.environment()).isEqualTo(Environment.CLIENT);
            assertThat(meta.entrypoint()).isEqualTo("com.example.ParsedEntrypoint");
            assertThat(meta.license()).isEqualTo("Apache-2.0");
            assertThat(meta.authors()).containsExactly("Dev One", "Dev Two");
            assertThat(meta.contacts()).containsExactly("https://example.com", "mail@example.com");
            assertThat(meta.dependencies())
                    .containsEntry("core-lib", ">=1.0.0")
                    .containsEntry("optional-dep", "^2.3");
            assertThat(meta.mixins()).containsExactly("mixins.parsed.class");
        }

        @Test
        @DisplayName("Should resolve wildcard environment string to Environment.BOTH")
        void shouldHandleWildcardEnvironment() throws IOException {
            var yaml = """
                    schema: %d
                    id: wildcard-plugin
                    name: Wildcard
                    version: 1.0.0
                    environment: "*"
                    """.formatted(SCHEMA);

            var node = loadYamlNode(yaml);
            var meta = Metadata.fromNode(node);

            assertThat(meta.environment()).isEqualTo(Environment.BOTH);
        }

        @Test
        @DisplayName("Should populate defaults for omitted optional fields")
        void shouldHandleMissingOptionalFields() throws IOException {
            var yaml = """
                    schema: %d
                    id: minimal-node
                    name: Minimal Node
                    version: 1.0.0
                    """.formatted(SCHEMA);

            var node = loadYamlNode(yaml);
            var meta = Metadata.fromNode(node);

            assertThat(meta.id()).isEqualTo("minimal-node");
            assertThat(meta.description()).isEmpty();
            assertThat(meta.license()).isEqualTo("UNLICENSED");
            assertThat(meta.entrypoint()).isEmpty();
            assertThat(meta.authors()).isEmpty();
            assertThat(meta.contacts()).isEmpty();
            assertThat(meta.dependencies()).isEmpty();
            assertThat(meta.mixins()).isEmpty();
        }
    }

    @Nested
    @DisplayName("fromGameProvider Mapping Tests")
    class FromGameProviderTests {

        @Test
        @DisplayName("Should synthesize valid Metadata from active GameProvider")
        void shouldMapProviderFields() {
            var provider = new TestGameProvider(
                    "project-zomboid",
                    "Project Zomboid",
                    "42.20.2",
                    Environment.CLIENT,
                    "zombie.gameStates.MainScreenState",
                    List.of("The Indie Stone"),
                    "PROPRIETARY",
                    List.of("https://projectzomboid.com")
            );

            var meta = Metadata.fromGameProvider(provider);

            assertThat(meta.schema()).isEqualTo(SCHEMA);
            assertThat(meta.id()).isEqualTo("project-zomboid");
            assertThat(meta.name()).isEqualTo("Project Zomboid");
            assertThat(meta.version()).isEqualTo("42.20.2");
            assertThat(meta.environment()).isEqualTo(Environment.CLIENT);
            assertThat(meta.entrypoint()).isEqualTo("zombie.gameStates.MainScreenState");
            assertThat(meta.authors()).containsExactly("The Indie Stone");
            assertThat(meta.license()).isEqualTo("PROPRIETARY");
            assertThat(meta.contacts()).containsExactly("https://projectzomboid.com");
        }
    }

    @Nested
    @DisplayName("File & JAR Loading Tests")
    class FileLoadingTests {

        @TempDir
        Path tempDir;

        @Test
        @DisplayName("Should load metadata descriptor directly from YAML file on disk")
        void shouldLoadFromYamlFile() throws IOException {
            var yamlPath = tempDir.resolve("plugin.yml");
            var content = """
                    schema: %d
                    id: file-plugin
                    name: File Plugin
                    version: 4.0.0
                    entrypoint: com.file.Main
                    authors: [FileAuthor]
                    """.formatted(SCHEMA);

            Files.writeString(yamlPath, content, StandardCharsets.UTF_8);

            var meta = Metadata.fromYaml(yamlPath);

            assertThat(meta.id()).isEqualTo("file-plugin");
            assertThat(meta.version()).isEqualTo("4.0.0");
            assertThat(meta.authors()).containsExactly("FileAuthor");
            assertThat(meta.entrypoint()).isEqualTo("com.file.Main");
        }

        @Test
        @DisplayName("Should load metadata manifest from inside a physical JAR archive")
        void shouldLoadFromJarEntry() throws IOException {
            var jarFile = createTempJarWithMetadata("plugin.yml");
            var meta = Metadata.fromJarFile(jarFile, "plugin.yml");

            assertThat(meta.id()).isEqualTo("jar-plugin");
            assertThat(meta.name()).isEqualTo("JAR Plugin");
            assertThat(meta.entrypoint()).isEqualTo("com.jar.Main");
        }

        @Test
        @DisplayName("Should fail with FileNotFoundException when requested manifest entry is missing in JAR")
        void shouldFailOnMissingJarEntry() throws IOException {
            var jarFile = createTempJarWithMetadata("plugin.yml");

            assertThatThrownBy(() -> Metadata.fromJarFile(jarFile, "nonexistent.yml"))
                    .isInstanceOf(FileNotFoundException.class)
                    .hasMessageContaining("not found in JAR");
        }

        private File createTempJarWithMetadata(String entryName) throws IOException {
            var jarFile = tempDir.resolve("test-plugin.jar").toFile();
            try (var jos = new JarOutputStream(Files.newOutputStream(jarFile.toPath()))) {
                var entry = new JarEntry(entryName);
                jos.putNextEntry(entry);
                var content = """
                        schema: %d
                        id: jar-plugin
                        name: JAR Plugin
                        version: 1.0.0
                        entrypoint: com.jar.Main
                        """.formatted(SCHEMA);
                jos.write(content.getBytes(StandardCharsets.UTF_8));
                jos.closeEntry();
            }
            return jarFile;
        }
    }

    /**
     * Minimal stub implementation of {@link GameProvider} for unit testing purposes.
     */
    private static final class TestGameProvider implements GameProvider {
        private final String id;
        private final String name;
        private final String version;
        private final Environment env;
        private final String entrypoint;
        private final List<String> authors;
        private final String license;
        private final List<String> contacts;

        TestGameProvider(String id, String name, String version, Environment env,
                         String entrypoint, List<String> authors, String license, List<String> contacts) {
            this.id = id;
            this.name = name;
            this.version = version;
            this.env = env;
            this.entrypoint = entrypoint;
            this.authors = authors;
            this.license = license;
            this.contacts = contacts;
        }

        @Override
        public String getId() {
            return id;
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
            return name;
        }

        @Override
        public String getNormalizedVersion() {
            return version;
        }

        @Override
        public String getRawVersion() {
            return version;
        }

        @Override
        public Path getLaunchDirectory() {
            return Paths.get(".").toAbsolutePath().normalize();
        }

        @Override
        public Environment getEnvironment() {
            return env;
        }

        @Override
        public String getEntrypoint() {
            return entrypoint;
        }

        @Override
        public List<String> getAuthors() {
            return authors;
        }

        @Override
        public String getLicense() {
            return license;
        }

        @Override
        public List<String> getContacts() {
            return contacts;
        }
    }
}