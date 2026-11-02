package com.avrix.plugins;

import com.avrix.core.Environment;
import com.avrix.core.Metadata;
import com.avrix.utils.Constants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Branch-oriented tests for {@link PluginData} guards, archive failures, and extraction edge cases.
 */
@DisplayName("PluginData Branch Coverage Tests")
class PluginDataBranchesTest {

    private static final int SCHEMA = Constants.METADATA_SCHEMA;

    @TempDir
    Path tempDir;

    private Metadata metadata;
    private Path pluginJar;

    @BeforeEach
    void setUp() throws IOException {
        metadata = new Metadata.Builder()
                .schema(SCHEMA)
                .id("branch-plugin")
                .name("Branch Plugin")
                .version("1.2.3")
                .environment(Environment.BOTH)
                .build();

        pluginJar = tempDir.resolve("branch-plugin.jar");
        try (JarOutputStream jar = new JarOutputStream(Files.newOutputStream(pluginJar))) {
            addEntry(jar, "assets/", "");
            addEntry(jar, "assets/config.json", "CONFIG");
            addEntry(jar, "assets/nested/deep.txt", "DEEP");
            addEntry(jar, "assets/empty/", "");
            addEntry(jar, "tree/sub/", "");
            addEntry(jar, "tree/sub/file.txt", "TREE");
            addEntry(jar, "root.txt", "ROOT");
            addEntry(jar, "icon.png", "ICON");
            addEntry(jar, "space dir/file name.txt", "SPACE");
        }
    }

    @Test
    @DisplayName("Should treat a null legacy File as a synthetic plugin")
    void shouldTreatNullLegacyFileAsSynthetic() {
        var pluginData = new PluginData((File) null, null, null, metadata);

        assertThat(pluginData.isSynthetic()).isTrue();
        assertThat(pluginData.getPluginPath()).isEmpty();
        assertThat(pluginData.getPluginFile()).isEmpty();
    }

    @Test
    @DisplayName("Should reject null, blank, and traversal-only resource paths")
    void shouldRejectBlankAndTraversalOnlyPaths() throws IOException {
        var pluginData = new PluginData(pluginJar, null, null, metadata);
        Path target = tempDir.resolve("blocked.txt");

        assertThat(pluginData.hasResource(null)).isFalse();
        assertThat(pluginData.hasResource("")).isFalse();
        assertThat(pluginData.hasResource("   ")).isFalse();
        assertThat(pluginData.hasResource("/")).isFalse();
        assertThat(pluginData.hasResource(".")).isFalse();
        assertThat(pluginData.hasResource("..")).isFalse();

        assertThat(pluginData.readResourceBytes(null)).isEmpty();
        assertThat(pluginData.readResourceBytes("")).isEmpty();
        assertThat(pluginData.readResourceBytes("   ")).isEmpty();
        assertThat(pluginData.readResourceBytes("/")).isEmpty();
        assertThat(pluginData.readResourceBytes(".")).isEmpty();

        assertThat(pluginData.openResource(null)).isEmpty();
        assertThat(pluginData.openResource("/")).isEmpty();

        assertThat(pluginData.getResourceURI(null)).isEmpty();
        assertThat(pluginData.getResourceURI("")).isEmpty();
        assertThat(pluginData.getResourceURI("   ")).isEmpty();
        assertThat(pluginData.getResourceURI("/")).isEmpty();
        assertThat(pluginData.getResourceURI(".")).isEmpty();

        assertThat(pluginData.extractResource(null, target, true)).isFalse();
        assertThat(pluginData.extractResource("", target, true)).isFalse();
        assertThat(pluginData.extractResource("   ", target, true)).isFalse();
        assertThat(pluginData.extractResource("/", target, true)).isFalse();
    }

    @Test
    @DisplayName("Should degrade inspection and discovery when the archive is corrupt")
    void shouldDegradeForCorruptArchive() throws IOException {
        Path corruptJar = tempDir.resolve("corrupt.jar");
        Files.writeString(corruptJar, "not a valid zip archive");
        var pluginData = new PluginData(corruptJar, null, null, metadata);

        assertThat(pluginData.hasResource("assets/config.json")).isFalse();
        assertThat(pluginData.listResources("assets", true)).isEmpty();
        assertThat(pluginData.findResources(name -> true)).isEmpty();
    }

    @Test
    @DisplayName("Should extract a directory with trailing slash and skip its blank directory entry")
    void shouldExtractDirectoryWithTrailingSlash() throws IOException {
        var pluginData = new PluginData(pluginJar, null, null, metadata);
        Path target = tempDir.resolve("exported-assets");

        int extracted = pluginData.extractDirectory("assets/", target, true);

        assertThat(extracted).isEqualTo(2);
        assertThat(target.resolve("config.json")).hasContent("CONFIG");
        assertThat(target.resolve("nested/deep.txt")).hasContent("DEEP");
        assertThat(target.resolve("empty")).isDirectory();
    }

    @Test
    @DisplayName("Should create explicit directory entries while extracting")
    void shouldCreateExplicitDirectoryEntries() throws IOException {
        var pluginData = new PluginData(pluginJar, null, null, metadata);
        Path target = tempDir.resolve("exported-tree");

        int extracted = pluginData.extractDirectory("tree", target, true);

        assertThat(extracted).isEqualTo(1);
        assertThat(target.resolve("sub")).isDirectory();
        assertThat(target.resolve("sub/file.txt")).hasContent("TREE");
    }

    @Test
    @DisplayName("Should extract every archive entry when the directory path is empty")
    void shouldExtractFromArchiveRoot() throws IOException {
        var pluginData = new PluginData(pluginJar, null, null, metadata);
        Path target = tempDir.resolve("exported-root");

        int extracted = pluginData.extractDirectory("", target, true);

        assertThat(extracted).isEqualTo(6);
        assertThat(target.resolve("root.txt")).hasContent("ROOT");
        assertThat(target.resolve("assets/config.json")).hasContent("CONFIG");
        assertThat(target.resolve("tree/sub/file.txt")).hasContent("TREE");
    }

    @Test
    @DisplayName("Should honor the overwrite flag when extracting a directory")
    void shouldHonorOverwriteDuringDirectoryExtraction() throws IOException {
        var pluginData = new PluginData(pluginJar, null, null, metadata);
        Path target = Files.createDirectories(tempDir.resolve("overwrite-target"));
        Path config = target.resolve("config.json");
        Path deep = Files.createDirectories(target.resolve("nested")).resolve("deep.txt");
        Files.writeString(config, "OLD");
        Files.writeString(deep, "OLD");

        assertThat(pluginData.extractDirectory("assets", target, false)).isZero();
        assertThat(config).hasContent("OLD");
        assertThat(deep).hasContent("OLD");

        assertThat(pluginData.extractDirectory("assets", target, true)).isEqualTo(2);
        assertThat(config).hasContent("CONFIG");
        assertThat(deep).hasContent("DEEP");
    }

    @Test
    @DisplayName("Should return zero for a null directory path on a physical plugin")
    void shouldReturnZeroForNullDirectoryPath() throws IOException {
        var pluginData = new PluginData(pluginJar, null, null, metadata);

        assertThat(pluginData.extractDirectory(null, tempDir.resolve("unused"), true)).isZero();
    }

    @Test
    @DisplayName("Should list resources when the directory path already ends with a slash")
    void shouldListResourcesWithTrailingSlash() {
        var pluginData = new PluginData(pluginJar, null, null, metadata);

        assertThat(pluginData.listResources("assets/", false))
                .containsExactly("assets/config.json");

        assertThat(pluginData.listResources("assets/", true))
                .containsExactlyInAnyOrder("assets/config.json", "assets/nested/deep.txt");
    }

    @Test
    @DisplayName("Should open an icon stream from a file-scheme URI")
    void shouldOpenIconStreamFromFileUri() throws IOException {
        Path icon = tempDir.resolve("external-icon.png");
        Files.writeString(icon, "FILE_ICON", StandardCharsets.UTF_8);
        var pluginData = new PluginData(pluginJar, icon.toUri(), null, metadata);

        try (InputStream stream = pluginData.openIconStream().orElseThrow()) {
            assertThat(new String(stream.readAllBytes(), StandardCharsets.UTF_8)).isEqualTo("FILE_ICON");
        }
    }

    @Test
    @DisplayName("Should distinguish identity, foreign types, and synthetic state in equality helpers")
    void shouldImplementEqualityHelpers() {
        var pluginData = new PluginData(pluginJar, null, null, metadata);

        assertThat(pluginData).isEqualTo(pluginData);
        assertThat(pluginData).isNotEqualTo("branch-plugin");
        assertThat(pluginData).isNotEqualTo(null);
        assertThat(pluginData.hashCode()).isEqualTo(Objects.hash("branch-plugin", "1.2.3"));
        assertThat(pluginData.toString())
                .contains("branch-plugin", "1.2.3", "synthetic=false");

        assertThat(new PluginData(metadata).toString()).contains("synthetic=true");
    }

    private static void addEntry(JarOutputStream jar, String entryName, String content) throws IOException {
        jar.putNextEntry(new JarEntry(entryName));
        if (!entryName.endsWith("/")) {
            jar.write(content.getBytes(StandardCharsets.UTF_8));
        }
        jar.closeEntry();
    }
}
