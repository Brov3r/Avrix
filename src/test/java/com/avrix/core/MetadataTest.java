package com.avrix.core;

import com.avrix.provider.GameProvider;
import com.avrix.utils.Constants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Metadata Unit Tests")
class MetadataTest {

    private static final int SCHEMA = Constants.METADATA_SCHEMA;

    @Nested
    @DisplayName("Constructor & Getters")
    class ConstructorTests {

        @Test
        @DisplayName("Should create instance and return correct values")
        void shouldCreateInstance() {
            var deps = Map.of("core", ">=1.0.0");
            var authors = List.of("Author One", "Author Two");
            var mixins = List.of("mixins.core.class");

            var meta = new Metadata(
                    SCHEMA, "Test Plugin", "A test plugin", "test-plugin", "1.2.3",
                    Environment.SERVER, authors, "MIT", List.of("dev@example.com"),
                    deps, "com.example.PluginMain", mixins
            );

            assertThat(meta.getSchema()).isEqualTo(SCHEMA);
            assertThat(meta.getName()).isEqualTo("Test Plugin");
            assertThat(meta.getDescription()).isEqualTo("A test plugin");
            assertThat(meta.getId()).isEqualTo("test-plugin");
            assertThat(meta.getVersion()).isEqualTo("1.2.3");
            assertThat(meta.getEnvironment()).isEqualTo(Environment.SERVER);
            assertThat(meta.getAuthors()).containsExactly("Author One", "Author Two");
            assertThat(meta.getLicense()).isEqualTo("MIT");
            assertThat(meta.getContacts()).containsExactly("dev@example.com");
            assertThat(meta.getDependencies()).containsEntry("core", ">=1.0.0");
            assertThat(meta.getEntrypoint()).isEqualTo("com.example.PluginMain");
            assertThat(meta.getMixins()).containsExactly("mixins.core.class");
        }

        @Test
        @DisplayName("Should return unmodifiable collections")
        void shouldReturnUnmodifiableCollections() {
            var meta = new Metadata(
                    SCHEMA, "Name", "Desc", "id", "1.0", Environment.BOTH,
                    List.of("A"), "MIT", List.of("C"), Map.of("D", "1.0"),
                    "EntryPoint", List.of("Mixin")
            );

            assertThatThrownBy(() -> meta.getAuthors().add("Hacker"))
                    .isInstanceOf(UnsupportedOperationException.class);
            assertThatThrownBy(() -> meta.getContacts().clear())
                    .isInstanceOf(UnsupportedOperationException.class);
            assertThatThrownBy(() -> meta.getDependencies().put("X", "Y"))
                    .isInstanceOf(UnsupportedOperationException.class);
            assertThatThrownBy(() -> meta.getMixins().removeFirst())
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    @DisplayName("Builder")
    class BuilderTests {

        @Test
        @DisplayName("Should build valid metadata with minimal fields")
        void shouldBuildWithMinimalFields() {
            var meta = new Metadata.Builder()
                    .schema(SCHEMA)
                    .id("minimal-id")
                    .name("Minimal")
                    .version("0.1.0")
                    .build();

            assertThat(meta.getId()).isEqualTo("minimal-id");
            assertThat(meta.getEnvironment()).isEqualTo(Environment.BOTH); // default
            assertThat(meta.getAuthors()).isEmpty();
            assertThat(meta.getDependencies()).isEmpty();
        }

        @Test
        @DisplayName("Should fail on unsupported schema version")
        void shouldFailOnWrongSchema() {
            assertThatThrownBy(() -> new Metadata.Builder()
                    .schema(999)
                    .id("id").name("name").version("1.0")
                    .build())
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Unsupported metadata schema version");
        }

        @Test
        @DisplayName("Should fail on missing required fields")
        void shouldFailOnMissingFields() {
            var base = new Metadata.Builder().schema(SCHEMA);

            assertThatThrownBy(base::build)
                    .hasMessageContaining("'id' cannot be null");
            assertThatThrownBy(() -> base.id("id").build())
                    .hasMessageContaining("'name' cannot be null");
            assertThatThrownBy(() -> base.id("id").name("name").build())
                    .hasMessageContaining("'version' cannot be null");
            assertThatThrownBy(() -> base.id("id").name("name").version("  ").build())
                    .hasMessageContaining("'version' cannot be null");
        }

        @Test
        @DisplayName("Should defensively copy collections")
        void shouldDefensivelyCopyCollections() {
            var authors = new java.util.ArrayList<>(List.of("Original"));
            var deps = new java.util.HashMap<>(Map.of("dep", "1.0"));

            var builder = new Metadata.Builder()
                    .schema(SCHEMA)
                    .id("id").name("name").version("1.0")
                    .authors(authors)
                    .dependencies(deps);

            // Modify originals
            authors.add("Hacker");
            deps.put("evil", "666");

            var meta = builder.build();

            assertThat(meta.getAuthors()).containsOnly("Original");
            assertThat(meta.getDependencies()).doesNotContainKey("evil");
        }

        @Test
        @DisplayName("Should support varargs and add methods")
        void shouldSupportConvenienceMethods() {
            var meta = new Metadata.Builder()
                    .schema(SCHEMA)
                    .id("id").name("name").version("1.0")
                    .authors("A1", "A2")
                    .addAuthor("A3")
                    .contacts("c1@example.com")
                    .addContact("c2@example.com")
                    .addDependency("lib-a", ">=2.0")
                    .addDependency("lib-b", "^1.0")
                    .mixins("mixin.a.class", "mixin.b.class")
                    .addMixin("mixin.c.class")
                    .build();

            assertThat(meta.getAuthors()).containsExactly("A1", "A2", "A3");
            assertThat(meta.getContacts()).containsExactly("c1@example.com", "c2@example.com");
            assertThat(meta.getDependencies())
                    .containsEntry("lib-a", ">=2.0")
                    .containsEntry("lib-b", "^1.0");
            assertThat(meta.getMixins()).hasSize(3);
        }
    }

    @Nested
    @DisplayName("fromNode")
    class FromNodeTests {

        private ConfigurationNode loadNode(String yaml) throws IOException {
            var loader = YamlConfigurationLoader.builder()
                    .source(() -> new BufferedReader(new StringReader(yaml)))
                    .build();
            return loader.load();
        }

        @Test
        @DisplayName("Should parse complete metadata from YAML node")
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

            var node = loadNode(yaml);
            var meta = Metadata.fromNode(node);

            assertThat(meta.getId()).isEqualTo("parsed-plugin");
            assertThat(meta.getName()).isEqualTo("Parsed Plugin");
            assertThat(meta.getVersion()).isEqualTo("2.5.1");
            assertThat(meta.getEnvironment()).isEqualTo(Environment.CLIENT);
            assertThat(meta.getEntrypoint()).isEqualTo("com.example.ParsedEntrypoint");
            assertThat(meta.getAuthors()).containsExactly("Dev One", "Dev Two");
            assertThat(meta.getDependencies())
                    .containsEntry("core-lib", ">=1.0.0")
                    .containsEntry("optional-dep", "^2.3");
            assertThat(meta.getMixins()).containsExactly("mixins.parsed.class");
        }

        @Test
        @DisplayName("Should handle wildcard environment as BOTH")
        void shouldHandleWildcardEnvironment() throws IOException {
            var yaml = """
                    schema: %d
                    id: id
                    name: name
                    version: 1.0
                    environment: "*"
                    """.formatted(SCHEMA);

            var node = loadNode(yaml);
            var meta = Metadata.fromNode(node);

            assertThat(meta.getEnvironment()).isEqualTo(Environment.BOTH);
        }

        @Test
        @DisplayName("Should handle missing optional fields")
        void shouldHandleMissingOptionalFields() throws IOException {
            var yaml = """
                    schema: %d
                    id: minimal
                    name: Minimal
                    version: 1.0.0
                    """.formatted(SCHEMA);

            var node = loadNode(yaml);
            var meta = Metadata.fromNode(node);

            assertThat(meta.getDescription()).isNull();
            assertThat(meta.getAuthors()).isEmpty();
            assertThat(meta.getDependencies()).isEmpty();
            assertThat(meta.getMixins()).isEmpty();
        }
    }

    @Nested
    @DisplayName("fromGameProvider")
    class FromGameProviderTests {

        @Test
        @DisplayName("Should map GameProvider fields to Metadata")
        void shouldMapProviderFields() {
            var provider = new TestGameProvider(
                    "prov-id", "Provider Plugin", "3.2.1",
                    Environment.CLIENT, "com.prov.Entrypoint",
                    List.of("ProvDev"), "MIT", List.of("prov@example.com")
            );

            var meta = Metadata.fromGameProvider(provider);

            assertThat(meta.getId()).isEqualTo("prov-id");
            assertThat(meta.getName()).isEqualTo("Provider Plugin");
            assertThat(meta.getVersion()).isEqualTo("3.2.1");
            assertThat(meta.getEnvironment()).isEqualTo(Environment.CLIENT);
            assertThat(meta.getEntrypoint()).isEqualTo("com.prov.Entrypoint");
            assertThat(meta.getAuthors()).containsExactly("ProvDev");
            assertThat(meta.getLicense()).isEqualTo("MIT");
        }
    }

    @Nested
    @DisplayName("File Loading")
    class FileLoadingTests {

        @TempDir
        Path tempDir;

        @Test
        @DisplayName("Should load metadata from YAML file")
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

            java.nio.file.Files.writeString(yamlPath, content, StandardCharsets.UTF_8);

            var meta = Metadata.fromYaml(yamlPath);

            assertThat(meta.getId()).isEqualTo("file-plugin");
            assertThat(meta.getVersion()).isEqualTo("4.0.0");
            assertThat(meta.getAuthors()).containsExactly("FileAuthor");
        }

        @Test
        @DisplayName("Should load metadata from JAR entry")
        void shouldLoadFromJarEntry() throws IOException {
            var jarPath = createTempJarWithMetadata();
            var meta = Metadata.fromJarFile(jarPath, "plugin.yml");

            assertThat(meta.getId()).isEqualTo("jar-plugin");
            assertThat(meta.getName()).isEqualTo("JAR Plugin");
            assertThat(meta.getEntrypoint()).isEqualTo("com.jar.Main");
        }

        @Test
        @DisplayName("Should throw FileNotFoundException for missing entry")
        void shouldFailOnMissingJarEntry() throws IOException {
            var jarPath = createTempJarWithMetadata();

            assertThatThrownBy(() -> Metadata.fromJarFile(jarPath, "nonexistent.yml"))
                    .isInstanceOf(java.io.FileNotFoundException.class)
                    .hasMessageContaining("Metadata entry not found");
        }

        private File createTempJarWithMetadata() throws IOException {
            var jarFile = tempDir.resolve("test-plugin.jar").toFile();
            try (var jos = new JarOutputStream(java.nio.file.Files.newOutputStream(jarFile.toPath()))) {
                var entry = new JarEntry("plugin.yml");
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
     * Simple test implementation of GameProvider for testing purposes.
     * Replace with real implementation or anonymous class as needed.
     */
    private static class TestGameProvider implements GameProvider {
        private final String id, name, version, entrypoint, license;
        private final Environment env;
        private final List<String> authors, contacts;

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
        public void init(BaseClassLoader classLoader) {

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
            return "";
        }

        @Override
        public Path getLaunchDirectory() {
            return null;
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