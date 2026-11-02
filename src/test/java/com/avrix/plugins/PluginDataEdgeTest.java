package com.avrix.plugins;

import com.avrix.core.Environment;
import com.avrix.core.Metadata;
import com.avrix.utils.Constants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Edge-case tests for {@link PluginData} resource lookup, path normalization, and icon handling.
 */
@DisplayName("PluginData Edge Case Tests")
class PluginDataEdgeTest {

    private static final int SCHEMA = Constants.METADATA_SCHEMA;

    @TempDir
    Path tempDir;

    private Path jarPath;
    private Metadata metadata;
    private boolean originalJarUseCaches;

    @BeforeEach
    void setUp() throws IOException {
        // The jar URL handler caches open JarFiles by default, which keeps archive locks on Windows.
        originalJarUseCaches = URLConnection.getDefaultUseCaches("jar");
        URLConnection.setDefaultUseCaches("jar", false);

        metadata = new Metadata.Builder()
                .schema(SCHEMA)
                .id("edge-plugin")
                .name("Edge Plugin")
                .version("1.0.0")
                .environment(Environment.BOTH)
                .build();

        jarPath = tempDir.resolve("edge-plugin.jar");
        try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(jarPath))) {
            addTextEntry(jos, "assets/config.json", "{\"key\":\"value\"}");
            addTextEntry(jos, "assets/top.txt", "TOP");
            addTextEntry(jos, "assets/nested/top.txt", "NESTED_TOP");
            addTextEntry(jos, "assets/nested/deep/file.txt", "DEEP");
            addTextEntry(jos, "root.txt", "ROOT");
            addTextEntry(jos, "icon.png", "PNG_ICON");
            addTextEntry(jos, "space dir/file name.txt", "SPACED");
            addBytesEntry(jos, "latin1.txt", "café".getBytes(StandardCharsets.ISO_8859_1));

            jos.putNextEntry(new JarEntry("assets/empty/"));
            jos.closeEntry();
        }
    }

    @AfterEach
    void tearDown() {
        URLConnection.setDefaultUseCaches("jar", originalJarUseCaches);
    }

    @Test
    @DisplayName("Should degrade every resource API safely for synthetic plugins")
    void shouldDegradeSafelyForSyntheticPlugins() throws IOException {
        var synthetic = new PluginData(metadata);

        assertThat(synthetic.isSynthetic()).isTrue();
        assertThat(synthetic.hasResource("assets/config.json")).isFalse();
        assertThat(synthetic.openResource("assets/config.json")).isEmpty();
        assertThat(synthetic.readResourceBytes("assets/config.json")).isEmpty();
        assertThat(synthetic.readResourceString("assets/config.json")).isEmpty();
        assertThat(synthetic.extractResource("assets/config.json", tempDir.resolve("out.json"), true)).isFalse();
        assertThat(synthetic.extractDirectory("assets", tempDir.resolve("out"), true)).isZero();
        assertThat(synthetic.listResources("assets", true)).isEmpty();
        assertThat(synthetic.findResources(name -> true)).isEmpty();
        assertThat(synthetic.getResourceURI("assets/config.json")).isEmpty();
        assertThat(synthetic.openIconStream()).isEmpty();
    }

    @Test
    @DisplayName("Should open a jar-scheme icon and surface a missing icon as an I/O failure")
    void shouldHandleJarIconUrisAndMissingIcons() throws IOException {
        String jarBase = "jar:" + jarPath.toUri().toASCIIString() + "!/";

        var existingIcon = new PluginData(jarPath, URI.create(jarBase + "icon.png"), null, metadata);
        try (InputStream in = existingIcon.openIconStream().orElseThrow()) {
            assertThat(new String(in.readAllBytes(), StandardCharsets.UTF_8)).isEqualTo("PNG_ICON");
        }

        var missingJarIcon = new PluginData(jarPath, URI.create(jarBase + "missing-icon.png"), null, metadata);
        assertThatThrownBy(missingJarIcon::openIconStream).isInstanceOf(IOException.class);

        var missingRelativeIcon = new PluginData(jarPath, URI.create("missing-icon.png"), null, metadata);
        assertThat(missingRelativeIcon.openIconStream()).isEmpty();

        var noIcon = new PluginData(jarPath, null, null, metadata);
        assertThat(noIcon.openIconStream()).isEmpty();
    }

    @Test
    @DisplayName("Should decode a non-UTF-8 resource with the requested charset")
    void shouldReadResourceWithExplicitCharset() throws IOException {
        var pluginData = new PluginData(jarPath, null, null, metadata);

        assertThat(pluginData.readResourceString("latin1.txt", StandardCharsets.ISO_8859_1))
                .hasValueSatisfying(value -> assertThat(value).isEqualTo("café"));

        String decodedAsUtf8 = pluginData.readResourceString("latin1.txt").orElseThrow();
        assertThat(decodedAsUtf8).isNotEqualTo("café");

        assertThatThrownBy(() -> pluginData.readResourceString("latin1.txt", null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Charset cannot be null");
    }

    @Test
    @DisplayName("Should build a jar resource URI, normalize separators, and encode spaces")
    void shouldBuildNormalizedResourceUri() {
        var pluginData = new PluginData(jarPath, null, null, metadata);

        URI uri = pluginData.getResourceURI("assets\\config.json").orElseThrow();

        assertThat(uri.getScheme()).isEqualTo("jar");
        assertThat(uri.toString()).startsWith("jar:file:");
        assertThat(uri.toString()).contains("!/assets/config.json");

        URI spacedUri = pluginData.getResourceURI("space dir/file name.txt").orElseThrow();
        assertThat(spacedUri.toString()).contains("%20");

        assertThat(pluginData.getResourceURI("")).isEmpty();
        assertThat(pluginData.getResourceURI(null)).isEmpty();
    }

    @Test
    @DisplayName("Should list resources recursively, non-recursively, and from the archive root")
    void shouldListResourcesWithAndWithoutRecursion() {
        var pluginData = new PluginData(jarPath, null, null, metadata);

        assertThat(pluginData.listResources("assets", false))
                .containsExactlyInAnyOrder("assets/config.json", "assets/top.txt");

        assertThat(pluginData.listResources("assets", true))
                .containsExactlyInAnyOrder(
                        "assets/config.json",
                        "assets/top.txt",
                        "assets/nested/top.txt",
                        "assets/nested/deep/file.txt"
                );

        assertThat(pluginData.listResources("assets/nested", false))
                .containsExactly("assets/nested/top.txt");

        assertThat(pluginData.listResources("", false))
                .contains("root.txt", "icon.png", "latin1.txt")
                .doesNotContain("assets/top.txt");

        assertThat(pluginData.listResources("", true))
                .contains("root.txt", "assets/nested/deep/file.txt");

        assertThat(pluginData.listResources("missing", true)).isEmpty();
        assertThat(pluginData.listResources(null, true)).isEmpty();
    }

    @Test
    @DisplayName("Should find resources by predicate and return an unmodifiable result")
    void shouldFindResourcesByPredicate() {
        var pluginData = new PluginData(jarPath, null, null, metadata);

        List<String> nestedMatches = pluginData.findResources(name -> name.contains("nested"));

        assertThat(nestedMatches).containsExactlyInAnyOrder(
                "assets/nested/top.txt",
                "assets/nested/deep/file.txt"
        );
        assertThatThrownBy(() -> nestedMatches.add("unexpected.txt"))
                .isInstanceOf(UnsupportedOperationException.class);

        assertThat(pluginData.findResources(null)).isEmpty();
        assertThat(pluginData.findResources(name -> false)).isEmpty();
    }

    @Test
    @DisplayName("Should honor overwrite flags and report missing resources during extraction")
    void shouldHandleExtractionOverwriteAndMissingResources() throws IOException {
        var pluginData = new PluginData(jarPath, null, null, metadata);
        Path target = tempDir.resolve("extracted/nested/config.json");

        assertThat(pluginData.extractResource("assets/config.json", target, false)).isTrue();
        assertThat(Files.readString(target)).isEqualTo("{\"key\":\"value\"}");

        Files.writeString(target, "MODIFIED");
        assertThat(pluginData.extractResource("assets/config.json", target, false)).isFalse();
        assertThat(Files.readString(target)).isEqualTo("MODIFIED");

        assertThat(pluginData.extractResource("assets\\config.json", target, true)).isTrue();
        assertThat(Files.readString(target)).isEqualTo("{\"key\":\"value\"}");

        assertThat(pluginData.extractResource("missing.txt", tempDir.resolve("missing.txt"), true)).isFalse();

        assertThatThrownBy(() -> pluginData.extractResource("assets/config.json", null, true))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Destination path cannot be null");
    }

    @Test
    @DisplayName("Should normalize backslashes, leading slashes, and dot segments in resource paths")
    void shouldNormalizeResourcePaths() throws IOException {
        var pluginData = new PluginData(jarPath, null, null, metadata);

        assertThat(pluginData.hasResource("assets\\config.json")).isTrue();
        assertThat(pluginData.hasResource("\\assets/config.json")).isTrue();
        assertThat(pluginData.hasResource("./assets/./config.json")).isTrue();
        assertThat(pluginData.hasResource("assets//nested/../config.json")).isTrue();
        assertThat(pluginData.hasResource("assets/nested/../../assets/config.json")).isTrue();
        assertThat(pluginData.hasResource("../assets/config.json")).isTrue();

        assertThat(pluginData.readResourceString("assets\\nested\\..\\config.json"))
                .contains("{\"key\":\"value\"}");

        assertThat(pluginData.hasResource("assets/empty")).isFalse();
        assertThat(pluginData.hasResource("   ")).isFalse();
        assertThat(pluginData.hasResource(null)).isFalse();
    }

    private static void addTextEntry(JarOutputStream jos, String entryName, String textContent) throws IOException {
        addBytesEntry(jos, entryName, textContent.getBytes(StandardCharsets.UTF_8));
    }

    private static void addBytesEntry(JarOutputStream jos, String entryName, byte[] content) throws IOException {
        jos.putNextEntry(new JarEntry(entryName));
        jos.write(content);
        jos.closeEntry();
    }
}
