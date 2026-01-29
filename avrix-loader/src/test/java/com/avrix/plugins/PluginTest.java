package com.avrix.plugins;

import com.avrix.utils.Constants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.junit.jupiter.api.Assertions.*;

public class PluginTest {

    @TempDir
    Path tempDir;

    private File cleanupFolder;

    @AfterEach
    public void cleanup() throws Exception {
        if (cleanupFolder == null) {
            return;
        }
        Path root = cleanupFolder.toPath();
        if (Files.exists(root)) {
            try (var walk = Files.walk(root)) {
                walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                    try {
                        Files.deleteIfExists(p);
                    } catch (IOException ignored) {
                        // ignore
                    }
                });
            }
        }
    }

    @Test
    public void getDefaultConfig_lazyLoads() throws Exception {
        Path jar = createJar(
                tempDir.resolve("plugin.jar"),
                Constants.PLUGINS_DEFAULT_CONFIG_NAME, "hello: world\n"
        );

        Metadata md = new Metadata.Builder()
                .name("Test Plugin")
                .id("test-plugin-" + System.nanoTime())
                .author("Test")
                .version("1.0.0")
                .license("MIT")
                .entryPoints(List.of("com.example.TestEntryPoint"))
                .pluginFile(jar.toFile())
                .build();

        Plugin plugin = new TestPlugin(md);
        cleanupFolder = plugin.getConfigFolder();

        assertNotNull(plugin.getDefaultConfig());

        Path cfgPath = plugin.getConfigFolder().toPath().resolve(Constants.PLUGINS_DEFAULT_CONFIG_NAME);
        assertTrue(Files.exists(cfgPath));
        assertEquals("world", plugin.getDefaultConfig().getString("hello"));
    }

    @Test
    public void loadConfig_rejectsTraversal_unixAndWindowsStyles() throws Exception {
        Path jar = createJar(tempDir.resolve("plugin.jar"));

        Metadata md = new Metadata.Builder()
                .name("Test Plugin")
                .id("test-plugin-" + System.nanoTime())
                .author("Test")
                .version("1.0.0")
                .license("MIT")
                .entryPoints(List.of("com.example.TestEntryPoint"))
                .pluginFile(jar.toFile())
                .build();

        Plugin plugin = new TestPlugin(md);
        cleanupFolder = plugin.getConfigFolder();

        assertThrows(IllegalArgumentException.class, () -> plugin.loadConfig("../evil"));
        assertThrows(IllegalArgumentException.class, () -> plugin.loadConfig("..\\evil"));
        assertThrows(IllegalArgumentException.class, () -> plugin.loadConfig("/abs/path"));
    }

    @Test
    public void metadata_createFromJar_readsMetadataYml() throws Exception {
        String metadataYml = """
                name: Test Plugin
                description: Some description
                id: test-plugin-%d
                author: Test Author
                version: 1.2.3
                license: MIT
                contacts: https://example.com
                environment: BOTH
                type: PLUGIN
                entrypoints:
                  - com.example.TestEntryPoint
                patches:
                  - com.example.Patch1
                dependencies:
                  dep-a: ">=1.0.0"
                """.formatted(System.nanoTime());

        Path jar = createJar(
                tempDir.resolve("plugin-with-metadata.jar"),
                Constants.PLUGINS_METADATA_NAME, metadataYml
        );

        Optional<Metadata> opt = Metadata.createFromJar(jar.toFile(), Constants.PLUGINS_METADATA_NAME);
        assertTrue(opt.isPresent(), "Expected metadata to be present");

        Metadata md = opt.get();
        assertEquals("Test Plugin", md.getName());
        assertEquals("Some description", md.getDescription());
        assertNotNull(md.getId());
        assertTrue(md.getId().startsWith("test-plugin-"));
        assertEquals("Test Author", md.getAuthor());
        assertEquals("1.2.3", md.getVersion());
        assertEquals("MIT", md.getLicense());
        assertEquals("https://example.com", md.getContacts());
        assertEquals(jar.toFile(), md.getPluginFile());

        assertEquals(List.of("com.example.TestEntryPoint"), md.getEntryPoints());
        assertEquals(List.of("com.example.Patch1"), md.getPatches());
        assertEquals(">=1.0.0", md.getDependencies().get("dep-a"));
    }

    @Test
    public void metadata_createFromJar_missingMetadata_returnsEmpty() throws Exception {
        Path jar = createJar(tempDir.resolve("plugin-no-metadata.jar"));

        Optional<Metadata> opt = Metadata.createFromJar(jar.toFile(), Constants.PLUGINS_METADATA_NAME);
        assertTrue(opt.isEmpty(), "Expected empty metadata when metadata.yml is missing or empty");
    }

    private static Path createJar(Path jarPath, String... entryAndContentPairs) throws IOException {
        if (entryAndContentPairs.length % 2 != 0) {
            throw new IllegalArgumentException("entryAndContentPairs must contain pairs: entryName, content");
        }

        try (OutputStream os = Files.newOutputStream(jarPath);
             JarOutputStream jos = new JarOutputStream(os)) {

            for (int i = 0; i < entryAndContentPairs.length; i += 2) {
                String entryName = entryAndContentPairs[i];
                String content = entryAndContentPairs[i + 1];

                jos.putNextEntry(new JarEntry(entryName));
                jos.write(content.getBytes(StandardCharsets.UTF_8));
                jos.closeEntry();
            }
        }
        return jarPath;
    }

    private static final class TestPlugin extends Plugin {
        private TestPlugin(Metadata metadata) {
            super(metadata);
        }

        @Override
        public void onInitialize() {
            // no-op
        }

        @Override
        public void onLaunch() {
            // no-op
        }
    }
}
