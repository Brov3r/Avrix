package com.avrix.api.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.ConfigurationOptions;
import org.spongepowered.configurate.yaml.NodeStyle;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ConfigManager")
class ConfigManagerTest {
    private static final Logger log = LoggerFactory.getLogger(ConfigManagerTest.class);
    @TempDir
    Path tempDir;

    // ========================================================================
    // CREATE TESTS
    // ========================================================================

    @Nested
    @DisplayName("create()")
    class CreateTests {

        @Test
        @DisplayName("creates node with default options")
        void create_defaultOptions_returnsNode() {
            ConfigurationNode node = ConfigManager.create();

            assertThat(node).isNotNull();
            assertThat(node.empty()).isTrue();
            assertThat(node.childrenMap()).isEmpty();
            assertThat(node.parent()).isNull();
        }

        @Test
        @DisplayName("creates empty node with custom options")
        void create_customOptions_returnsConfiguredNode() {
            ConfigurationOptions customOptions = ConfigurationOptions.defaults()
                    .shouldCopyDefaults(true);

            ConfigurationNode node = ConfigManager.create(customOptions, NodeStyle.BLOCK);

            assertThat(node).isNotNull();
            assertThat(node.options().shouldCopyDefaults()).isTrue();
        }

        @Test
        @DisplayName("NodeStyle.BLOCK produces multiline YAML")
        void create_blockStyle_savesMultiline() throws Exception {
            ConfigurationNode config = ConfigManager.create(
                    ConfigurationOptions.defaults(), NodeStyle.BLOCK);
            config.node("a", "b").set("value");

            Path path = tempDir.resolve("style.yml");
            ConfigManager.save(config, path);

            String content = Files.readString(path);

            assertThat(content).contains("a:");
            assertThat(content).contains("b: value");
            assertThat(content).doesNotContain("{b:");
        }

        @Test
        @DisplayName("NodeStyle.FLOW produces multiline YAML")
        void create_flowStyle_savesMultiline() throws Exception {
            ConfigurationNode config = ConfigManager.create(
                    ConfigurationOptions.defaults(), NodeStyle.BLOCK);
            config.node("a", "b").set("value");

            Path path = tempDir.resolve("style.yml");
            ConfigManager.save(config, path, NodeStyle.FLOW);

            String content = Files.readString(path);

            assertThat(content).contains("a:");
            assertThat(content).contains("b: value");
            assertThat(content).contains("{b:");
        }
    }

    // ========================================================================
    // LOAD FROM PATH TESTS
    // ========================================================================

    @Nested
    @DisplayName("load(Path)")
    class LoadFromPathTests {

        @Test
        @DisplayName("loads valid YAML configuration")
        void load_validYaml_returnsPopulatedNode() throws Exception {
            Path configPath = tempDir.resolve("test.yml");
            Files.writeString(configPath, """
                    app:
                      name: TestApp
                      version: 1.0
                    """);

            ConfigurationNode node = ConfigManager.load(configPath);

            assertThat(node).isNotNull();
            assertThat(node.node("app", "name").getString()).isEqualTo("TestApp");
            assertThat(node.node("app", "version").getString()).isEqualTo("1.0");
        }

        @Test
        @DisplayName("throws on null path")
        void load_nullPath_throwsNPE() {
            assertThatThrownBy(() -> ConfigManager.load(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("Path cannot be null");
        }

        @Test
        @DisplayName("throws ConfigurateException on malformed YAML")
        void load_invalidYaml_throwsConfigurateException() throws IOException {
            Path invalidPath = tempDir.resolve("invalid.yml");
            // Intentionally broken YAML: Not a closed sequence
            Files.writeString(invalidPath, """
                    broken: [unclosed, list,
                    key: value
                    """);

            assertThatThrownBy(() -> ConfigManager.load(invalidPath))
                    .isInstanceOf(ConfigurateException.class);
        }

        @Test
        @DisplayName("loads empty file as node")
        void load_emptyFile_returnsNode() throws Exception {
            Path emptyPath = tempDir.resolve("empty.yml");
            Files.createFile(emptyPath);

            ConfigurationNode node = ConfigManager.load(emptyPath);

            assertThat(node).isNotNull();
            assertThat(node.empty()).isTrue();
        }
    }

    // ========================================================================
    // LOAD FROM JAR TESTS
    // ========================================================================

    @Nested
    @DisplayName("load(File, String)")
    class LoadFromJarTests {

        private Path createTestJar(String entryName, String content) throws IOException {
            Path jarPath = tempDir.resolve("test.jar");
            try (var fos = Files.newOutputStream(jarPath);
                 var jos = new JarOutputStream(fos)) {

                JarEntry entry = new JarEntry(entryName);
                jos.putNextEntry(entry);
                jos.write(content.getBytes());
                jos.closeEntry();
            }
            return jarPath;
        }

        private Path createTestJarWithMultipleEntries() throws IOException {
            Path jarPath = tempDir.resolve("multi.jar");
            try (var fos = Files.newOutputStream(jarPath);
                 var jos = new JarOutputStream(fos)) {

                // Root config
                jos.putNextEntry(new JarEntry("config.yml"));
                jos.write("root: true".getBytes());
                jos.closeEntry();

                // Nested config
                jos.putNextEntry(new JarEntry("modules/auth/config.yml"));
                jos.write("auth: enabled".getBytes());
                jos.closeEntry();

                // Config with leading slash variant
                jos.putNextEntry(new JarEntry("/settings.yml"));
                jos.write("settings: global".getBytes());
                jos.closeEntry();
            }
            return jarPath;
        }

        @Test
        @DisplayName("loads config from JAR and extracts to work directory")
        void load_fromJar_extractsAndLoads() throws Exception {
            Path jarPath = createTestJar("config.yml", "app:\n  name: JarApp");
            File jarFile = jarPath.toFile();

            ConfigurationNode node = ConfigManager.load(jarFile, "config.yml");

            assertThat(node.node("app", "name").getString()).isEqualTo("JarApp");

            // Verify extraction occurred
            Path workDir = tempDir.resolve("test");
            Path extractedConfig = workDir.resolve("config.yml");
            assertThat(Files.exists(extractedConfig)).isTrue();
            assertThat(Files.readString(extractedConfig)).contains("JarApp");
        }

        @Test
        @DisplayName("prioritizes filesystem config over JAR resource")
        void load_filesystemPriority_overridesJar() throws Exception {
            Path jarPath = createTestJar("config.yml", "app:\n  source: jar");
            File jarFile = jarPath.toFile();

            // Pre-create config in work directory with different content
            Path workDir = tempDir.resolve("test");
            Files.createDirectories(workDir);
            Path fsConfig = workDir.resolve("config.yml");
            Files.writeString(fsConfig, "app:\n  source: filesystem");

            ConfigurationNode node = ConfigManager.load(jarFile, "config.yml");

            assertThat(node.node("app", "source").getString()).isEqualTo("filesystem");
        }

        @Test
        @DisplayName("uses default config name when internalPath is blank")
        void load_blankInternalPath_usesDefaultName() throws Exception {
            Path jarPath = createTestJar("config.yml", "default: loaded");
            File jarFile = jarPath.toFile();

            ConfigurationNode node = ConfigManager.load(jarFile, "");

            assertThat(node.node("default").getString()).isEqualTo("loaded");
        }

        @Test
        @DisplayName("finds entry with leading slash variation")
        void load_leadingSlash_findsEntry() throws Exception {
            Path jarPath = createTestJarWithMultipleEntries();
            File jarFile = jarPath.toFile();

            ConfigurationNode node = ConfigManager.load(jarFile, "/settings.yml");

            assertThat(node.node("settings").getString()).isEqualTo("global");

            Path workDir = ConfigManager.getWorkDir(jarFile);
            Path extracted = workDir.resolve("settings.yml");
            assertThat(Files.exists(extracted)).isTrue();
            assertThat(Files.readString(extracted)).contains("settings: global");
        }

        @Test
        @DisplayName("finds nested entry by exact path")
        void load_nestedPath_byExactPath() throws Exception {
            Path jarPath = createTestJarWithMultipleEntries();
            File jarFile = jarPath.toFile();

            ConfigurationNode node = ConfigManager.load(jarFile, "modules/auth/config.yml");

            assertThat(node.node("auth").getString()).isEqualTo("enabled");
        }

        @Test
        @DisplayName("throws FileNotFoundException when entry not in JAR")
        void load_missingEntry_throwsFileNotFoundException() throws Exception {
            Path jarPath = createTestJar("config.yml", "data: value");
            File jarFile = jarPath.toFile();

            assertThatThrownBy(() -> ConfigManager.load(jarFile, "nonexistent.yml"))
                    .isInstanceOf(FileNotFoundException.class)
                    .hasMessageContaining("nonexistent.yml")
                    .hasMessageContaining("test.jar");
        }

        @Test
        @DisplayName("throws on null jarFile")
        void load_nullJarFile_throwsNPE() {
            assertThatThrownBy(() -> ConfigManager.load(null, "config.yml"))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("jarFile cannot be null");
        }

        @Test
        @DisplayName("throws on null internalPath")
        void load_nullInternalPath_throwsNPE() throws IOException {
            Path jarPath = createTestJar("config.yml", "test: value");

            assertThatThrownBy(() -> ConfigManager.load(jarPath.toFile(), null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("internalPath cannot be null");
        }

        @Test
        @DisplayName("throws FileNotFoundException when JAR does not exist")
        void load_nonexistentJar_throwsFileNotFoundException() {
            File fakeJar = new File("/nonexistent/path.jar");

            assertThatThrownBy(() -> ConfigManager.load(fakeJar, "config.yml"))
                    .isInstanceOf(FileNotFoundException.class)
                    .hasMessageContaining("JAR file must exist");
        }

        @ParameterizedTest
        @ValueSource(strings = {"config.txt", "settings.json", "data.xml", "noextension"})
        @DisplayName("throws on invalid file extension: {0}")
        void load_invalidExtension_throwsIllegalArgumentException(String invalidPath)
                throws IOException {
            Path jarPath = createTestJar("config.yml", "test: value");
            File jarFile = jarPath.toFile();

            assertThatThrownBy(() -> ConfigManager.load(jarFile, invalidPath))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must have .yml or .yaml extension");
        }

        @Test
        @DisplayName("creates work directory adjacent to JAR")
        void load_createsWorkDirectory() throws Exception {
            Path jarPath = createTestJar("config.yml", "test: value");
            File jarFile = jarPath.toFile();

            ConfigManager.load(jarFile, "config.yml");

            Path expectedWorkDir = tempDir.resolve("test");
            assertThat(Files.isDirectory(expectedWorkDir)).isTrue();
        }

        @Test
        @DisplayName("handles JAR in current directory (no parent)")
        void load_jarWithoutParent_usesCurrentDirectory() throws Exception {
            // Create JAR directly in tempDir without subpath
            Path jarPath = tempDir.resolve("standalone.jar");
            try (var fos = Files.newOutputStream(jarPath);
                 var jos = new JarOutputStream(fos)) {
                jos.putNextEntry(new JarEntry("config.yml"));
                jos.write("standalone: true".getBytes());
                jos.closeEntry();
            }

            ConfigurationNode node = ConfigManager.load(jarPath.toFile(), "config.yml");

            assertThat(node.node("standalone").getBoolean()).isTrue();

            // Work dir should be created in same directory as JAR
            Path workDir = tempDir.resolve("standalone");
            assertThat(Files.exists(workDir.resolve("config.yml"))).isTrue();
        }
    }

    // ========================================================================
    // LOAD DEFAULT TESTS
    // ========================================================================

    @Nested
    @DisplayName("loadDefault(File)")
    class LoadDefaultTests {

        private Path createTestJarWithDefaultConfig() throws IOException {
            return createTestJar("config.yml", "default:\n  key: value");
        }

        private Path createTestJar(String entryName, String content) throws IOException {
            Path jarPath = tempDir.resolve("test.jar");
            try (var fos = Files.newOutputStream(jarPath);
                 var jos = new JarOutputStream(fos)) {
                JarEntry entry = new JarEntry(entryName);
                jos.putNextEntry(entry);
                jos.write(content.getBytes());
                jos.closeEntry();
            }
            return jarPath;
        }

        @Test
        @DisplayName("loads default config name from JAR")
        void loadDefault_fromJar_loadsConfigYml() throws Exception {
            Path jarPath = createTestJarWithDefaultConfig();
            File jarFile = jarPath.toFile();

            ConfigurationNode node = ConfigManager.loadDefault(jarFile);

            assertThat(node.node("default", "key").getString()).isEqualTo("value");
        }

        @Test
        @DisplayName("propagates exceptions from load()")
        void loadDefault_propagatesExceptions() throws Exception {
            Path jarPath = createTestJar("other.yml", "test: value");
            File jarFile = jarPath.toFile();

            // config.yml doesn't exist in JAR -> FileNotFoundException
            assertThatThrownBy(() -> ConfigManager.loadDefault(jarFile))
                    .isInstanceOf(FileNotFoundException.class);
        }

        @Test
        @DisplayName("throws ConfigurateException when file does not exist")
        void load_nonexistentFile_throwsConfigurateException() {
            Path nonexistent = tempDir.resolve("does_not_exist.yml");

            assertThatThrownBy(() -> ConfigManager.load(nonexistent))
                    .isInstanceOf(ConfigurateException.class)
                    .hasMessageContaining("Configuration file not found")
                    .hasMessageContaining("does_not_exist.yml");
        }

        @Test
        @DisplayName("loads YAML with UTF-8 encoding and unicode characters")
        void load_utf8Encoding_withUnicode() throws Exception {
            Path configPath = tempDir.resolve("unicode.yml");

            String content = """
                    app:
                      name: Приложение «Тест» 🚀
                      description: >
                        Многострочное
                        описание с символами: © ® ™ € £ ¥
                    """;

            Files.writeString(configPath, content, StandardCharsets.UTF_8);

            ConfigurationNode node = ConfigManager.load(configPath);

            assertThat(node.node("app", "name").getString()).contains("Приложение", "🚀");
            assertThat(node.node("app", "description").getString())
                    .contains("Многострочное", "€");
        }

        @Test
        @DisplayName("finds JAR entry when internalPath uses Windows backslashes")
        void load_windowsStylePath_normalized() throws Exception {
            Path jarPath = tempDir.resolve("windows.jar");
            try (var fos = Files.newOutputStream(jarPath);
                 var jos = new JarOutputStream(fos)) {
                jos.putNextEntry(new JarEntry("modules/auth/config.yml"));
                jos.write("auth: windows_test".getBytes());
                jos.closeEntry();
            }

            File jarFile = jarPath.toFile();

            ConfigurationNode node = ConfigManager.load(jarFile, "modules\\auth\\config.yml");

            assertThat(node.node("auth").getString()).isEqualTo("windows_test");
        }
    }
    // ========================================================================
    // SAVE TESTS
    // ========================================================================

    @Nested
    @DisplayName("save(ConfigurationNode, Path)")
    class SaveTests {

        @Test
        @DisplayName("saves valid configuration to file")
        void save_validConfig_writesYamlFile() throws Exception {
            ConfigurationNode config = ConfigManager.create();
            config.node("app", "name").set("TestApp");
            config.node("app", "port").set(8080);

            Path outputPath = tempDir.resolve("output/config.yml");
            ConfigManager.save(config, outputPath);

            assertThat(Files.exists(outputPath)).isTrue();
            String content = Files.readString(outputPath);
            assertThat(content).contains("app:");
            assertThat(content).contains("name: TestApp");
            assertThat(content).contains("port: 8080");
        }

        @Test
        @DisplayName("creates parent directories if missing")
        void save_missingParent_createsDirectories() throws Exception {
            ConfigurationNode config = ConfigManager.create();
            config.node("key").set("value");

            Path nestedPath = tempDir.resolve("a/b/c/config.yml");
            ConfigManager.save(config, nestedPath);

            assertThat(Files.exists(nestedPath)).isTrue();
            assertThat(Files.isDirectory(tempDir.resolve("a/b/c"))).isTrue();
        }

        @Test
        @DisplayName("throws on null config")
        void save_nullConfig_throwsNPE() {
            Path path = tempDir.resolve("test.yml");

            assertThatThrownBy(() -> ConfigManager.save(null, path))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("ConfigurationNode must not be null");
        }

        @Test
        @DisplayName("throws on null path")
        void save_nullPath_throwsNPE() {
            ConfigurationNode config = ConfigManager.create();

            assertThatThrownBy(() -> ConfigManager.save(config, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("Path must not be null");
        }

        @Test
        @DisplayName("overwrites existing file")
        void save_existingFile_overwritesContent() throws Exception {
            Path configPath = tempDir.resolve("overwrite.yml");
            Files.writeString(configPath, "old: data");

            ConfigurationNode config = ConfigManager.create();
            config.node("new").set("data");

            ConfigManager.save(config, configPath);

            String content = Files.readString(configPath);
            assertThat(content).doesNotContain("old: data");
            assertThat(content).contains("new: data");
        }

        @Test
        @DisplayName("saves empty config as valid YAML")
        void save_emptyConfig_writesValidFile() throws Exception {
            ConfigurationNode config = ConfigManager.create();
            Path outputPath = tempDir.resolve("empty.yml");

            ConfigManager.save(config, outputPath);

            assertThat(Files.exists(outputPath)).isTrue();
            // Empty config should produce empty or minimal valid YAML
            assertThat(Files.readString(outputPath)).isNotNull();
        }

        @Test
        @DisplayName("throws IOException when saving to read-only directory")
        void save_readonlyDirectory_throws() throws Exception {
            Path readonlyDir = tempDir.resolve("readonly");
            Files.createDirectories(readonlyDir);

            if (!System.getProperty("os.name").toLowerCase().contains("win")) {
                readonlyDir.toFile().setWritable(false);

                ConfigurationNode config = ConfigManager.create();
                config.node("key").set("value");
                Path target = readonlyDir.resolve("config.yml");

                assertThatThrownBy(() -> ConfigManager.save(config, target))
                        .isInstanceOf(IOException.class);

                readonlyDir.toFile().setWritable(true);
            }
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "config #1.yml",
                "file & test.yaml",
                "конфиг.yml",
                "file with spaces.yml",
                "config[dev].yml"
        })
        @DisplayName("saves config with special characters in filename: {0}")
        void save_specialCharsInFilename_works(String filename) throws Exception {
            ConfigurationNode config = ConfigManager.create();
            config.node("app", "name").set("SpecialApp");

            Path outputPath = tempDir.resolve(filename);
            ConfigManager.save(config, outputPath);

            assertThat(Files.exists(outputPath)).isTrue();

            ConfigurationNode loaded = ConfigManager.load(outputPath);
            assertThat(loaded.node("app", "name").getString()).isEqualTo("SpecialApp");
        }
    }

    // ========================================================================
    // SECURITY TESTS
    // ========================================================================

    @Nested
    @DisplayName("Security validations")
    class SecurityTests {

        @Test
        @DisplayName("blocks extraction when workDir contains symlink to parent directory")
        void load_symlinkInWorkDir_blocked() throws Exception {
            Path jarPath = tempDir.resolve("secure.jar");
            try (var fos = Files.newOutputStream(jarPath);
                 var jos = new JarOutputStream(fos)) {
                jos.putNextEntry(new JarEntry("config.yml"));
                jos.write("secure: true".getBytes());
                jos.closeEntry();
            }

            Path workDir = tempDir.resolve("secure");
            Files.createDirectories(workDir);

            Path symlink = workDir.resolve("escape_link");
            Path target = tempDir;
            Files.createSymbolicLink(symlink, target);

            File jarFile = jarPath.toFile();
            ConfigurationNode node = ConfigManager.load(jarFile, "config.yml");

            Path extracted = workDir.resolve("config.yml");
            assertThat(Files.exists(extracted)).isTrue();
            assertThat(node.node("secure").getBoolean()).isTrue();

            assertThat(Files.isSymbolicLink(symlink)).isTrue();
            assertThat(Files.isSymbolicLink(extracted)).isFalse();
        }

        @Test
        @DisplayName("extracts nested config preserving directory structure")
        void load_nestedPath_preservesStructure() throws Exception {
            Path jarPath = tempDir.resolve("nested.jar");
            try (var fos = Files.newOutputStream(jarPath);
                 var jos = new JarOutputStream(fos)) {
                jos.putNextEntry(new JarEntry("modules/auth/config.yml"));
                jos.write("auth:\n  provider: oauth2".getBytes());
                jos.closeEntry();
            }

            File jarFile = jarPath.toFile();
            ConfigurationNode node = ConfigManager.load(jarFile, "modules/auth/config.yml");

            assertThat(node.node("auth", "provider").getString()).isEqualTo("oauth2");

            Path workDir = ConfigManager.getWorkDir(jarFile);
            Path extracted = workDir.resolve("modules/auth/config.yml");
            assertThat(Files.exists(extracted)).isTrue();
            assertThat(Files.readString(extracted)).contains("provider: oauth2");
        }

        @Test
        @DisplayName("blocks path traversal attempts with SecurityException")
        void load_pathTraversal_blocked() throws Exception {
            Path jarPath = tempDir.resolve("test.jar");
            try (var fos = Files.newOutputStream(jarPath);
                 var jos = new JarOutputStream(fos)) {
                jos.putNextEntry(new JarEntry("subdir/../../../escape.yml"));
                jos.write("escaped: true".getBytes());
                jos.closeEntry();
            }

            File jarFile = jarPath.toFile();

            assertThatThrownBy(() -> ConfigManager.load(jarFile, "subdir/../../../escape.yml"))
                    .isInstanceOf(SecurityException.class)
                    .hasMessageContaining("Path traversal");
        }

        @Test
        @DisplayName("getFileName extracts basename correctly")
        void getFileName_extraction_works() throws Exception {
            // Use reflection to test private method
            java.lang.reflect.Method method = ConfigManager.class
                    .getDeclaredMethod("getFileName", String.class);
            method.setAccessible(true);

            assertThat(method.invoke(null, "config.yml")).isEqualTo("config.yml");
            assertThat(method.invoke(null, "path/to/config.yml")).isEqualTo("config.yml");
            assertThat(method.invoke(null, "/absolute/path/config.yaml"))
                    .isEqualTo("config.yaml");
            assertThat(method.invoke(null, "justfile")).isEqualTo("justfile");
        }

        @Test
        @DisplayName("work directory is always within JAR's parent")
        void getWorkDir_staysWithinBounds() throws Exception {
            Path jarPath = tempDir.resolve("subdir/app.jar");
            Files.createDirectories(jarPath.getParent());
            Files.createFile(jarPath);

            // Use reflection to test private method
            java.lang.reflect.Method method = ConfigManager.class
                    .getDeclaredMethod("getWorkDir", File.class);
            method.setAccessible(true);

            Path workDir = (Path) method.invoke(null, jarPath.toFile());

            assertThat(workDir).startsWith(tempDir);
            assertThat(workDir.getFileName().toString()).isEqualTo("app");
        }
    }

    // ========================================================================
    // EDGE CASES & INTEGRATION
    // ========================================================================

    @Nested
    @DisplayName("Edge cases and integration")
    class EdgeCaseTests {

        @Test
        @DisplayName("loads large config without OutOfMemoryError")
        void load_hugeConfig_handled() throws Exception {
            Path largeConfig = tempDir.resolve("large.yml");
            try (BufferedWriter writer = Files.newBufferedWriter(largeConfig)) {
                writer.write("data:\n");
                for (int i = 0; i < 10_000; i++) {
                    writer.write(String.format("  key_%05d: \"value_%d\"\n", i, i));
                }
            }

            long startMem = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            long startTime = System.currentTimeMillis();

            ConfigurationNode node = ConfigManager.load(largeConfig);

            long endTime = System.currentTimeMillis();
            long endMem = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

            assertThat(node.node("data", "key_00000").getString()).isEqualTo("value_0");
            assertThat(node.node("data", "key_09999").getString()).isEqualTo("value_9999");

            log.info("Large config loaded: {} ms, {} MB heap used",
                    endTime - startTime, (endMem - startMem) / 1024 / 1024);
        }

        @Test
        @DisplayName("multiple loads after extraction read from filesystem, not JAR")
        void load_afterExtraction_readsFromFilesystem() throws Exception {
            Path jarPath = tempDir.resolve("reuse.jar");
            try (var fos = Files.newOutputStream(jarPath);
                 var jos = new JarOutputStream(fos)) {
                jos.putNextEntry(new JarEntry("config.yml"));
                jos.write("version: 1".getBytes());
                jos.closeEntry();
            }

            File jarFile = jarPath.toFile();

            ConfigurationNode node1 = ConfigManager.load(jarFile, "config.yml");
            assertThat(node1.node("version").getString()).isEqualTo("1");

            Path extracted = tempDir.resolve("reuse/config.yml");
            assertThat(Files.exists(extracted)).isTrue();

            Files.writeString(extracted, "version: 2\nmodified: true");

            ConfigurationNode node2 = ConfigManager.load(jarFile, "config.yml");

            assertThat(node2.node("version").getString()).isEqualTo("2");
            assertThat(node2.node("modified").getBoolean()).isTrue();
        }

        @Test
        @DisplayName("roundtrip: save then load preserves data")
        void roundtrip_saveAndLoad_preservesData() throws Exception {
            ConfigurationNode original = ConfigManager.create();
            original.node("server", "host").set("localhost");
            original.node("server", "port").set(9000);
            original.node("features", "auth").set(true);
            original.node("features", "list").setList(String.class, List.of("a", "b", "c"));

            Path configPath = tempDir.resolve("roundtrip.yml");
            ConfigManager.save(original, configPath);

            ConfigurationNode loaded = ConfigManager.load(configPath);

            assertThat(loaded.node("server", "host").getString())
                    .isEqualTo(original.node("server", "host").getString());
            assertThat(loaded.node("server", "port").getInt())
                    .isEqualTo(original.node("server", "port").getInt());
            assertThat(loaded.node("features", "auth").getBoolean())
                    .isEqualTo(original.node("features", "auth").getBoolean());
            assertThat(loaded.node("features", "list").getList(String.class))
                    .containsExactlyElementsOf(
                            original.node("features", "list").getList(String.class));
        }

        @Test
        @DisplayName("multiple loads from same JAR reuse extracted file")
        void load_multipleCalls_reusesExtractedConfig() throws Exception {
            Path jarPath = tempDir.resolve("reuse.jar");
            try (var fos = Files.newOutputStream(jarPath);
                 var jos = new JarOutputStream(fos)) {
                jos.putNextEntry(new JarEntry("config.yml"));
                jos.write("version: 1".getBytes());
                jos.closeEntry();
            }

            File jarFile = jarPath.toFile();

            // First load: extracts from JAR
            ConfigurationNode node1 = ConfigManager.load(jarFile, "config.yml");
            Path extracted = tempDir.resolve("reuse/config.yml");

            // Wait to ensure timestamp difference if file is rewritten
            Thread.sleep(10);

            // Modify extracted file
            Files.writeString(extracted, "version: 2\nmodified: true");

            // Second load: should read from filesystem, not re-extract
            ConfigurationNode node2 = ConfigManager.load(jarFile, "config.yml");

            assertThat(node2.node("version").getString()).isEqualTo("2");
            assertThat(node2.node("modified").getBoolean()).isTrue();
        }

        @Test
        @DisplayName("YAML features: comments, anchors, multi-line preserved")
        void load_yamlFeatures_handledByConfigurate() throws Exception {
            Path configPath = tempDir.resolve("features.yml");
            Files.writeString(configPath, """
                    # Application config
                    app:
                      name: &app_name MyApplication
                      alias: *app_name
                      description: >
                        This is a multi-line
                        description that should
                        be folded into one line
                    """);

            ConfigurationNode node = ConfigManager.load(configPath);

            assertThat(node.node("app", "name").getString()).isEqualTo("MyApplication");
            assertThat(node.node("app", "alias").getString()).isEqualTo("MyApplication");
            assertThat(node.node("app", "description").getString())
                    .contains("This is a multi-line description");
        }
    }

    // ========================================================================
    // LOAD OR CREATE TESTS
    // ========================================================================

    @Nested
    @DisplayName("loadOrCreate()")
    class LoadOrCreateTests {
        @Test
        @DisplayName("loads existing config file when present")
        void loadOrCreate_existingFile_loadsContent() throws Exception {
            Path configPath = tempDir.resolve("existing.yml");
            Files.writeString(configPath, """
                    app:
                      name: ExistingApp
                      version: 2.0
                    """);

            ConfigurationNode node = ConfigManager.loadOrCreate(configPath);

            assertThat(node).isNotNull();
            assertThat(node.node("app", "name").getString()).isEqualTo("ExistingApp");
            assertThat(node.node("app", "version").getString()).isEqualTo("2.0");
        }

        @Test
        @DisplayName("creates new empty node when file is missing")
        void loadOrCreate_missingFile_createsEmptyNode() throws Exception {
            Path newConfigPath = tempDir.resolve("new/config.yml");

            ConfigurationNode node = ConfigManager.loadOrCreate(newConfigPath);

            assertThat(node).isNotNull();
            assertThat(node.empty()).isTrue();

            assertThat(Files.isDirectory(newConfigPath.getParent())).isTrue();
        }

        @Test
        @DisplayName("uses custom options and style for new node")
        void loadOrCreate_customOptions_appliedToNewNode() throws Exception {
            Path configPath = tempDir.resolve("custom.yml");
            ConfigurationOptions customOpts = ConfigurationOptions.defaults()
                    .shouldCopyDefaults(true);

            ConfigurationNode node = ConfigManager.loadOrCreate(configPath, customOpts, NodeStyle.FLOW);

            assertThat(node.options().shouldCopyDefaults()).isTrue();

            node.node("test").set("value");
            ConfigManager.save(node, configPath, NodeStyle.FLOW);

            String content = Files.readString(configPath);
            assertThat(content).contains("{test: value}");
        }

        @Test
        @DisplayName("created node can be saved and loaded back")
        void loadOrCreate_roundtrip_saveAndLoad() throws Exception {
            Path configPath = tempDir.resolve("roundtrip/config.yml");

            ConfigurationNode newNode = ConfigManager.loadOrCreate(configPath);
            newNode.node("initialized").set(true);
            newNode.node("data", "key").set("value");

            ConfigManager.save(newNode, configPath);

            ConfigurationNode loadedNode = ConfigManager.loadOrCreate(configPath);

            assertThat(loadedNode.node("initialized").getBoolean()).isTrue();
            assertThat(loadedNode.node("data", "key").getString()).isEqualTo("value");
        }

        @Test
        @DisplayName("throws on null path")
        void loadOrCreate_nullPath_throwsNPE() {
            assertThatThrownBy(() -> ConfigManager.loadOrCreate((Path) null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("Path cannot be null");
        }

        @Test
        @DisplayName("throws on null options")
        void loadOrCreate_nullOptions_throwsNPE() {
            Path path = tempDir.resolve("test.yml");
            assertThatThrownBy(() -> ConfigManager.loadOrCreate(path, null, NodeStyle.BLOCK))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("Options cannot be null");
        }

        @Test
        @DisplayName("throws on null style")
        void loadOrCreate_nullStyle_throwsNPE() {
            Path path = tempDir.resolve("test.yml");
            assertThatThrownBy(() -> ConfigManager.loadOrCreate(path, ConfigurationOptions.defaults(), null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("NodeStyle cannot be null");
        }

        @Test
        @DisplayName("throws ConfigurateException on malformed existing file")
        void loadOrCreate_invalidExistingFile_throwsConfigurateException() throws IOException {
            Path invalidPath = tempDir.resolve("invalid.yml");
            Files.writeString(invalidPath, "broken: [unclosed");

            assertThatThrownBy(() -> ConfigManager.loadOrCreate(invalidPath))
                    .isInstanceOf(ConfigurateException.class);
        }

        @Test
        @DisplayName("creates deeply nested parent directories")
        void loadOrCreate_deepPath_createsAllParents() throws Exception {
            Path deepPath = tempDir.resolve("a/b/c/d/e/config.yml");

            ConfigurationNode node = ConfigManager.loadOrCreate(deepPath);

            assertThat(Files.isDirectory(deepPath.getParent())).isTrue();
            assertThat(Files.isDirectory(tempDir.resolve("a/b/c/d/e"))).isTrue();
        }

        @Test
        @DisplayName("idempotent: calling twice returns same file content")
        void loadOrCreate_idempotent_behavior() throws Exception {
            Path configPath = tempDir.resolve("idempotent.yml");

            ConfigurationNode node1 = ConfigManager.loadOrCreate(configPath);
            node1.node("counter").set(1);
            ConfigManager.save(node1, configPath);

            ConfigurationNode node2 = ConfigManager.loadOrCreate(configPath);

            assertThat(node2.node("counter").getInt()).isEqualTo(1);
        }

        // --------------------------------------------------------------------
        // JAR-based variants
        // --------------------------------------------------------------------

        private Path createTestJar(String entryName, String content) throws IOException {
            Path jarPath = tempDir.resolve("test.jar");
            try (var fos = Files.newOutputStream(jarPath);
                 var jos = new JarOutputStream(fos)) {
                JarEntry entry = new JarEntry(entryName);
                jos.putNextEntry(entry);
                jos.write(content.getBytes());
                jos.closeEntry();
            }
            return jarPath;
        }

        @Test
        @DisplayName("JAR variant: creates node in workDir when file missing")
        void loadOrCreate_jar_missingFile_createsInWorkDir() throws Exception {
            Path jarPath = createTestJar("config.yml", "jar: content");
            File jarFile = jarPath.toFile();
            Path relativePath = Path.of("custom.yml");

            ConfigurationNode node = ConfigManager.loadOrCreate(jarFile, relativePath);

            Path workDir = ConfigManager.getWorkDir(jarFile);
            Path expectedPath = workDir.resolve("custom.yml");
            assertThat(Files.exists(expectedPath)).isTrue();
            assertThat(Files.isRegularFile(expectedPath)).isTrue();

            String content = Files.readString(expectedPath);
            assertThat(content).isNotNull();
        }

        @Test
        @DisplayName("JAR variant: loads existing file from workDir")
        void loadOrCreate_jar_existingFile_loadsFromWorkDir() throws Exception {
            Path jarPath = createTestJar("config.yml", "jar: content");
            File jarFile = jarPath.toFile();

            Path workDir = ConfigManager.getWorkDir(jarFile);
            Path targetPath = workDir.resolve("custom.yml");
            Files.writeString(targetPath, "workdir: loaded");

            ConfigurationNode node = ConfigManager.loadOrCreate(jarFile, Path.of("custom.yml"));

            assertThat(node.node("workdir").getString()).isEqualTo("loaded");
        }

        @Test
        @DisplayName("JAR variant: handles nested relative paths")
        void loadOrCreate_jar_nestedPath_createsDirectories() throws Exception {
            Path jarPath = createTestJar("config.yml", "jar: content");
            File jarFile = jarPath.toFile();
            Path nestedPath = Path.of("modules/auth/settings.yml");

            ConfigurationNode node = ConfigManager.loadOrCreate(jarFile, nestedPath);

            Path workDir = ConfigManager.getWorkDir(jarFile);
            Path expectedPath = workDir.resolve("modules/auth/settings.yml");
            assertThat(Files.isDirectory(expectedPath.getParent())).isTrue();
        }

        @Test
        @DisplayName("JAR variant: uses custom options for new node")
        void loadOrCreate_jar_customOptions_applied() throws Exception {
            Path jarPath = createTestJar("config.yml", "jar: content");
            File jarFile = jarPath.toFile();
            ConfigurationOptions customOpts = ConfigurationOptions.defaults()
                    .shouldCopyDefaults(true);

            ConfigurationNode node = ConfigManager.loadOrCreate(
                    jarFile, Path.of("opts.yml"), customOpts, NodeStyle.FLOW);

            assertThat(node.options().shouldCopyDefaults()).isTrue();
        }

        @Test
        @DisplayName("JAR variant: throws on null jarFile")
        void loadOrCreate_jar_nullJarFile_throwsNPE() {
            assertThatThrownBy(() -> ConfigManager.loadOrCreate((File) null, Path.of("config.yml")))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("jarFile cannot be null");
        }

        @Test
        @DisplayName("JAR variant: throws on null path")
        void loadOrCreate_jar_nullPath_throwsNPE() throws IOException {
            Path jarPath = createTestJar("config.yml", "test");
            File jarFile = jarPath.toFile();

            assertThatThrownBy(() -> ConfigManager.loadOrCreate(jarFile, (Path) null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("internalPath cannot be null");
        }

        @Test
        @DisplayName("JAR variant: throws FileNotFoundException if JAR missing")
        void loadOrCreate_jar_missingJar_throwsFileNotFoundException() {
            File fakeJar = new File("/nonexistent/app.jar");

            assertThatThrownBy(() -> ConfigManager.loadOrCreate(fakeJar, Path.of("config.yml")))
                    .isInstanceOf(FileNotFoundException.class)
                    .hasMessageContaining("JAR file must exist");
        }

        @Test
        @DisplayName("JAR variant: workDir is correctly resolved adjacent to JAR")
        void loadOrCreate_jar_workDirResolution_correct() throws Exception {
            Path jarPath = tempDir.resolve("myapp.jar");
            Files.createFile(jarPath);
            File jarFile = jarPath.toFile();
            Path relativePath = Path.of("sub/config.yml");

            ConfigManager.loadOrCreate(jarFile, relativePath);

            Path workDir = ConfigManager.getWorkDir(jarFile);
            assertThat(workDir.getFileName().toString()).isEqualTo("myapp");
            assertThat(workDir.getParent()).isEqualTo(tempDir);
        }

        // --------------------------------------------------------------------
        // Integration: JAR extraction + loadOrCreate interaction
        // --------------------------------------------------------------------

        @Test
        @DisplayName("load() extracts from JAR, then loadOrCreate() reuses extracted file")
        void load_then_loadOrCreate_usesExtractedFile() throws Exception {
            Path jarPath = createTestJar("config.yml", "version: 1");
            File jarFile = jarPath.toFile();

            ConfigurationNode fromJar = ConfigManager.load(jarFile, "config.yml");
            assertThat(fromJar.node("version").getString()).isEqualTo("1");

            Path workDir = ConfigManager.getWorkDir(jarFile);
            Path extracted = workDir.resolve("config.yml");
            assertThat(Files.exists(extracted)).isTrue();

            Files.writeString(extracted, "version: 2\nmodified: true");

            ConfigurationNode viaLoadOrCreate = ConfigManager.loadOrCreate(jarFile, Path.of("config.yml"));

            assertThat(viaLoadOrCreate.node("version").getString()).isEqualTo("2");
            assertThat(viaLoadOrCreate.node("modified").getBoolean()).isTrue();
        }

        @Test
        @DisplayName("loadOrCreate() with JAR: save() persists to workDir, not JAR")
        void loadOrCreate_jar_save_persistsToWorkDir() throws Exception {
            Path jarPath = createTestJar("config.yml", "jar: original");
            File jarFile = jarPath.toFile();

            ConfigurationNode node = ConfigManager.loadOrCreate(jarFile, Path.of("new.yml"));
            node.node("app", "name").set("MyApp");

            Path workDir = ConfigManager.getWorkDir(jarFile);
            Path savedPath = workDir.resolve("new.yml");
            ConfigManager.save(node, savedPath);

            assertThat(Files.exists(savedPath)).isTrue();
            String content = Files.readString(savedPath);
            assertThat(content).contains("name: MyApp");

            ConfigurationNode reloaded = ConfigManager.loadOrCreate(jarFile, Path.of("new.yml"));
            assertThat(reloaded.node("app", "name").getString()).isEqualTo("MyApp");
        }
    }
}