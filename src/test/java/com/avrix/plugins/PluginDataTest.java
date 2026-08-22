package com.avrix.plugins;

import com.avrix.core.Environment;
import com.avrix.core.Metadata;
import com.avrix.utils.Constants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Comprehensive test suite verifying runtime state, isolated resource resolution,
 * path traversal sanitization, and filesystem export routines in {@link PluginData}.
 */
@DisplayName("PluginData Unit & Integration Tests")
class PluginDataTest {

    private static final int SCHEMA = Constants.METADATA_SCHEMA;

    @TempDir
    Path tempDir;

    private Path sampleJarPath;
    private Metadata standardMetadata;

    @BeforeEach
    void setUp() throws IOException {
        standardMetadata = new Metadata.Builder()
                .schema(SCHEMA)
                .id("test-plugin")
                .name("Test Plugin")
                .version("1.0.0")
                .environment(Environment.BOTH)
                .build();

        sampleJarPath = tempDir.resolve("test-plugin-1.0.0.jar");
        createSamplePluginJar(sampleJarPath);
    }

    /**
     * Helper to generate a realistic plugin JAR archive containing structured assets.
     */
    private void createSamplePluginJar(Path destination) throws IOException {
        try (var jos = new JarOutputStream(Files.newOutputStream(destination))) {
            addEntry(jos, "plugin.yaml", "id: test-plugin\nversion: 1.0.0");
            addEntry(jos, "icon.png", "PNG_RAW_BYTES");

            addEntry(jos, "assets/config.json", "{\"key\":\"value\"}");
            addEntry(jos, "assets/lang/en_US.json", "{\"lang\":\"en\"}");
            addEntry(jos, "assets/lang/ru_RU.json", "{\"lang\":\"ru\"}");
            addEntry(jos, "media/lua/client/Main.lua", "print('Hello Client')");
            addEntry(jos, "media/lua/server/Server.lua", "print('Hello Server')");
            addEntry(jos, "media/textures/character/skin.png", "SKIN_IMAGE_BYTES");

            jos.putNextEntry(new JarEntry("media/empty_folder/"));
            jos.closeEntry();
        }
    }

    private void addEntry(JarOutputStream jos, String entryName, String textContent) throws IOException {
        var entry = new JarEntry(entryName);
        jos.putNextEntry(entry);
        jos.write(textContent.getBytes(StandardCharsets.UTF_8));
        jos.closeEntry();
    }

    @Nested
    @DisplayName("Constructor & Invariant Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should throw NullPointerException when metadata is null")
        void shouldThrowWhenMetadataIsNull() {
            assertThatThrownBy(() -> new PluginData(sampleJarPath, null, null, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("Plugin metadata cannot be null");
        }

        @Test
        @DisplayName("Should normalize physical JAR path to absolute path")
        void shouldNormalizeJarPath() {
            var pluginData = new PluginData(sampleJarPath, null, null, standardMetadata);

            assertThat(pluginData.getPluginPath()).isPresent();
            assertThat(pluginData.getPluginPath().get()).isAbsolute();
            assertThat(pluginData.getPluginFile()).isPresent();
        }

        @Test
        @DisplayName("Should support legacy File constructor and resolve internal Path")
        void shouldSupportFileConstructor() {
            File file = sampleJarPath.toFile();
            var pluginData = new PluginData(file, null, null, standardMetadata);

            assertThat(pluginData.getPluginPath()).contains(sampleJarPath.toAbsolutePath().normalize());
            assertThat(pluginData.getPluginFile()).contains(file.getAbsoluteFile());
        }

        @Test
        @DisplayName("Should initialize synthetic PluginData with only metadata")
        void shouldInitializeSyntheticContainer() {
            var synthetic = new PluginData(standardMetadata);

            assertThat(synthetic.isSynthetic()).isTrue();
            assertThat(synthetic.getPluginPath()).isEmpty();
            assertThat(synthetic.getPluginFile()).isEmpty();
            assertThat(synthetic.getPluginIconURI()).isEmpty();
            assertThat(synthetic.getPluginInstance()).isEmpty();
            assertThat(synthetic.id()).isEqualTo("test-plugin");
        }
    }

    @Nested
    @DisplayName("Accessors & Lifecycle Callback Tests")
    class AccessorsTests {

        @Test
        @DisplayName("Should return matching plugin identity, icon URI, and instance state")
        void shouldReturnStateAttributes() {
            var iconUri = URI.create("file:///tmp/icon.png");
            Plugin stubPlugin = data -> {
            };

            var pluginData = new PluginData(sampleJarPath, iconUri, stubPlugin, standardMetadata);

            assertThat(pluginData.id()).isEqualTo("test-plugin");
            assertThat(pluginData.metadata()).isSameAs(standardMetadata);
            assertThat(pluginData.getPluginIconURI()).contains(iconUri);
            assertThat(pluginData.getPluginInstance()).contains(stubPlugin);
            assertThat(pluginData.isSynthetic()).isFalse();
        }

        @Test
        @DisplayName("Should properly invoke onInitialize lifecycle callback with PluginData reference")
        void shouldTriggerPluginInitialization() {
            var initializedRef = new AtomicReference<PluginData>();
            var invokedFlag = new AtomicBoolean(false);

            Plugin plugin = data -> {
                invokedFlag.set(true);
                initializedRef.set(data);
            };

            var pluginData = new PluginData(sampleJarPath, null, plugin, standardMetadata);
            pluginData.getPluginInstance().ifPresent(inst -> inst.onInitialize(pluginData));

            assertThat(invokedFlag).isTrue();
            assertThat(initializedRef.get()).isSameAs(pluginData);
            assertThat(initializedRef.get().id()).isEqualTo("test-plugin");
        }

        @Test
        @DisplayName("Should report isSynthetic true if file path does not physically exist on disk")
        void shouldReportSyntheticForNonExistentFile() {
            var nonExistent = tempDir.resolve("missing.jar");
            var pluginData = new PluginData(nonExistent, null, null, standardMetadata);

            assertThat(pluginData.isSynthetic()).isTrue();
        }
    }

    @Nested
    @DisplayName("Resource Inspection & Reading Tests")
    class ResourceReadingTests {

        @Test
        @DisplayName("Should verify resource existence with hasResource")
        void shouldVerifyHasResource() {
            var pluginData = new PluginData(sampleJarPath, null, null, standardMetadata);

            assertThat(pluginData.hasResource("assets/config.json")).isTrue();
            assertThat(pluginData.hasResource("/assets/config.json")).isTrue();
            assertThat(pluginData.hasResource("assets\\config.json")).isTrue();
            assertThat(pluginData.hasResource("nonexistent.file")).isFalse();
            assertThat(pluginData.hasResource("media/empty_folder/")).isFalse();
            assertThat(pluginData.hasResource("")).isFalse();
            assertThat(pluginData.hasResource(null)).isFalse();
        }

        @Test
        @DisplayName("Should read resource bytes and open in-memory InputStream")
        void shouldReadBytesAndOpenStream() throws IOException {
            var pluginData = new PluginData(sampleJarPath, null, null, standardMetadata);

            var bytesOpt = pluginData.readResourceBytes("assets/config.json");
            assertThat(bytesOpt).isPresent();
            assertThat(new String(bytesOpt.get(), StandardCharsets.UTF_8)).isEqualTo("{\"key\":\"value\"}");

            var streamOpt = pluginData.openResource("assets/config.json");
            assertThat(streamOpt).isPresent();
            try (InputStream in = streamOpt.get()) {
                assertThat(new String(in.readAllBytes(), StandardCharsets.UTF_8)).isEqualTo("{\"key\":\"value\"}");
            }
        }

        @Test
        @DisplayName("Should read resource string with default UTF-8 and explicit Charset")
        void shouldReadStringWithEncodings() throws IOException {
            var pluginData = new PluginData(sampleJarPath, null, null, standardMetadata);

            var contentUtf8 = pluginData.readResourceString("assets/lang/ru_RU.json");
            assertThat(contentUtf8).contains("{\"lang\":\"ru\"}");

            var contentIso = pluginData.readResourceString("assets/lang/en_US.json", StandardCharsets.ISO_8859_1);
            assertThat(contentIso).contains("{\"lang\":\"en\"}");
        }

        @Test
        @DisplayName("Should return empty Optionals when querying synthetic plugin or missing resources")
        void shouldDegradeSafelyOnSyntheticOrMissing() throws IOException {
            var synthetic = new PluginData(standardMetadata);

            assertThat(synthetic.hasResource("assets/config.json")).isFalse();
            assertThat(synthetic.readResourceBytes("assets/config.json")).isEmpty();
            assertThat(synthetic.readResourceString("assets/config.json")).isEmpty();
            assertThat(synthetic.openResource("assets/config.json")).isEmpty();

            var realPlugin = new PluginData(sampleJarPath, null, null, standardMetadata);
            assertThat(realPlugin.readResourceBytes("missing/file.txt")).isEmpty();
            assertThat(realPlugin.readResourceString("missing/file.txt")).isEmpty();
            assertThat(realPlugin.openResource("missing/file.txt")).isEmpty();
        }
    }

    @Nested
    @DisplayName("Resource Extraction & Security Tests")
    class ResourceExtractionTests {

        @Test
        @DisplayName("Should extract single resource file to filesystem target")
        void shouldExtractSingleResource() throws IOException {
            var pluginData = new PluginData(sampleJarPath, null, null, standardMetadata);
            var targetFile = tempDir.resolve("extracted/config.json");

            boolean extracted = pluginData.extractResource("assets/config.json", targetFile, false);
            assertThat(extracted).isTrue();
            assertThat(Files.readString(targetFile)).isEqualTo("{\"key\":\"value\"}");

            Files.writeString(targetFile, "MODIFIED");
            boolean overwritten = pluginData.extractResource("assets/config.json", targetFile, false);
            assertThat(overwritten).isFalse();
            assertThat(Files.readString(targetFile)).isEqualTo("MODIFIED");

            boolean forceOverwritten = pluginData.extractResource("assets/config.json", targetFile, true);
            assertThat(forceOverwritten).isTrue();
            assertThat(Files.readString(targetFile)).isEqualTo("{\"key\":\"value\"}");
        }

        @Test
        @DisplayName("Should extract entire directory structure recursively")
        void shouldExtractDirectoryStructure() throws IOException {
            var pluginData = new PluginData(sampleJarPath, null, null, standardMetadata);
            var exportTarget = tempDir.resolve("exported_media");

            int extractedCount = pluginData.extractDirectory("media/lua", exportTarget, true);
            assertThat(extractedCount).isEqualTo(2);

            assertThat(exportTarget.resolve("client/Main.lua")).exists();
            assertThat(exportTarget.resolve("server/Server.lua")).exists();
            assertThat(Files.readString(exportTarget.resolve("client/Main.lua"))).isEqualTo("print('Hello Client')");
        }

        @Test
        @DisplayName("Should prevent Zip Slip directory traversal attacks during extraction")
        void shouldPreventZipSlipVulnerability() throws IOException {
            var maliciousJar = tempDir.resolve("malicious.jar");
            try (var jos = new JarOutputStream(Files.newOutputStream(maliciousJar))) {
                addEntry(jos, "safe/legit.txt", "LEGIT");
                addEntry(jos, "safe/../../escape.txt", "MALICIOUS_PAYLOAD");
            }

            var pluginData = new PluginData(maliciousJar, null, null, standardMetadata);
            var targetDir = tempDir.resolve("sandbox");
            Files.createDirectories(targetDir);

            int extracted = pluginData.extractDirectory("safe", targetDir, true);

            assertThat(extracted).isEqualTo(1);
            assertThat(targetDir.resolve("legit.txt")).exists();
            assertThat(tempDir.resolve("escape.txt")).doesNotExist();
        }
    }

    @Nested
    @DisplayName("Resource Discovery & URI Resolution Tests")
    class DiscoveryAndUriTests {

        @Test
        @DisplayName("Should list resources in directory non-recursively and recursively")
        void shouldListDirectoryResources() {
            var pluginData = new PluginData(sampleJarPath, null, null, standardMetadata);

            // Recursive listing
            List<String> recursive = pluginData.listResources("media/lua", true);
            assertThat(recursive).containsExactlyInAnyOrder(
                    "media/lua/client/Main.lua",
                    "media/lua/server/Server.lua"
            );

            List<String> flat = pluginData.listResources("assets", false);
            assertThat(flat).containsExactly("assets/config.json");
        }

        @Test
        @DisplayName("Should find resources matching predicate filter")
        void shouldFindResourcesWithFilter() {
            var pluginData = new PluginData(sampleJarPath, null, null, standardMetadata);

            List<String> luaFiles = pluginData.findResources(name -> name.endsWith(".lua"));
            assertThat(luaFiles).containsExactlyInAnyOrder(
                    "media/lua/client/Main.lua",
                    "media/lua/server/Server.lua"
            );

            List<String> pngFiles = pluginData.findResources(name -> name.endsWith(".png"));
            assertThat(pngFiles).containsExactlyInAnyOrder(
                    "icon.png",
                    "media/textures/character/skin.png"
            );
        }

        @Test
        @DisplayName("Should resolve compliant jar: URI with proper encoding")
        void shouldResolveJarUri() {
            var pluginData = new PluginData(sampleJarPath, null, null, standardMetadata);

            var uriOpt = pluginData.getResourceURI("assets/config.json");
            assertThat(uriOpt).isPresent();

            URI uri = uriOpt.get();
            assertThat(uri.getScheme()).isEqualTo("jar");
            assertThat(uri.toString()).contains("!/assets/config.json");
        }

        @Test
        @DisplayName("Should open icon stream from resolved internal JAR or external URI")
        void shouldOpenIconStream() throws IOException {
            var internalIconUri = URI.create("icon.png");
            var pluginWithInternalIcon = new PluginData(sampleJarPath, internalIconUri, null, standardMetadata);

            var iconStreamOpt = pluginWithInternalIcon.openIconStream();
            assertThat(iconStreamOpt).isPresent();
            try (InputStream in = iconStreamOpt.get()) {
                assertThat(new String(in.readAllBytes(), StandardCharsets.UTF_8)).isEqualTo("PNG_RAW_BYTES");
            }

            var pluginWithoutIcon = new PluginData(sampleJarPath, null, null, standardMetadata);
            assertThat(pluginWithoutIcon.openIconStream()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Equality, HashCode & ToString Tests")
    class EqualityTests {

        @Test
        @DisplayName("Should verify equality based on plugin ID and version")
        void shouldVerifyEqualsAndHashCode() {
            var metaA1 = new Metadata.Builder().schema(SCHEMA).id("mod-a").name("A").version("1.0.0").build();
            var metaA2 = new Metadata.Builder().schema(SCHEMA).id("mod-a").name("A").version("1.0.0").build();
            var metaB = new Metadata.Builder().schema(SCHEMA).id("mod-b").name("B").version("1.0.0").build();
            var metaA_v2 = new Metadata.Builder().schema(SCHEMA).id("mod-a").name("A").version("2.0.0").build();

            var plugin1 = new PluginData(sampleJarPath, null, null, metaA1);
            var plugin2 = new PluginData(metaA2);
            var pluginDiffId = new PluginData(sampleJarPath, null, null, metaB);
            var pluginDiffVersion = new PluginData(sampleJarPath, null, null, metaA_v2);

            assertThat(plugin1).isEqualTo(plugin2);
            assertThat(plugin1.hashCode()).isEqualTo(plugin2.hashCode());

            assertThat(plugin1).isNotEqualTo(pluginDiffId);
            assertThat(plugin1).isNotEqualTo(pluginDiffVersion);
        }

        @Test
        @DisplayName("Should produce informative toString representation")
        void shouldProduceFormattedToString() {
            var pluginData = new PluginData(sampleJarPath, null, null, standardMetadata);
            assertThat(pluginData.toString()).contains("test-plugin", "1.0.0", "synthetic=false");
        }
    }
}