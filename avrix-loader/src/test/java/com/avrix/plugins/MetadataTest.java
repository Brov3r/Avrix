package com.avrix.plugins;

import com.avrix.enums.Environment;
import com.avrix.enums.PluginType;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class MetadataTest {

    @Test
    void build_shouldCreateImmutableSnapshots() {
        List<String> entryPoints = new java.util.ArrayList<>(List.of("com.example.Main"));
        List<String> patches = new java.util.ArrayList<>(List.of("patch1"));
        Map<String, String> dependencies = new java.util.HashMap<>(Map.of("dep1", ">=1.0.0"));

        Metadata metadata = new Metadata.Builder()
                .name("Plugin")
                .id("plugin-id")
                .author("Author")
                .version("1.0.0")
                .license("MIT")
                .type(PluginType.PLUGIN)
                .environment(Environment.BOTH.name())
                .entryPoints(entryPoints)
                .patches(patches)
                .dependencies(dependencies)
                .build();

        entryPoints.add("com.example.Other");
        patches.add("patch2");
        dependencies.put("dep2", "2.0.0");

        assertEquals(List.of("com.example.Main"), metadata.getEntryPoints());
        assertEquals(List.of("patch1"), metadata.getPatches());
        assertEquals(Map.of("dep1", ">=1.0.0"), metadata.getDependencies());

        assertThrows(UnsupportedOperationException.class, () -> metadata.getEntryPoints().add("x"));
        assertThrows(UnsupportedOperationException.class, () -> metadata.getPatches().add("x"));
        assertThrows(UnsupportedOperationException.class, () -> metadata.getDependencies().put("x", "y"));
    }

    @Test
    void build_shouldNormalizeStringsAndCollections() {
        List<String> entryPoints = new ArrayList<>(Arrays.asList("  com.example.Main  ", null, "   ", "\t"));
        List<String> patches = new ArrayList<>(Arrays.asList("  patch1  ", "", "   ", null));

        Map<String, String> dependencies = new HashMap<>();
        dependencies.put(" dep1 ", " >=1.0.0 ");
        dependencies.put("   ", "x");      // blank key -> should be dropped
        dependencies.put("dep2", null);    // null value -> should become ""

        Metadata metadata = new Metadata.Builder()
                .name("  Plugin  ")
                .id("  plugin-id  ")
                .author("  Author  ")
                .version("  1.0.0  ")
                .license("  MIT  ")
                .contacts("  https://example.com  ")
                .type(PluginType.PLUGIN)
                .environment("  BOTH  ")
                .entryPoints(entryPoints)
                .patches(patches)
                .dependencies(dependencies)
                .build();

        assertEquals("Plugin", metadata.getName());
        assertEquals("plugin-id", metadata.getId());
        assertEquals("Author", metadata.getAuthor());
        assertEquals("1.0.0", metadata.getVersion());
        assertEquals("MIT", metadata.getLicense());
        assertEquals("https://example.com", metadata.getContacts());
        assertEquals(PluginType.PLUGIN, metadata.getType());
        assertEquals(Environment.BOTH, metadata.getEnvironment());

        assertEquals(List.of("com.example.Main"), metadata.getEntryPoints());
        assertEquals(List.of("patch1"), metadata.getPatches());
        assertEquals(Map.of("dep1", ">=1.0.0", "dep2", ""), metadata.getDependencies());
    }

    @Test
    void build_shouldRequireEntrypoints_forPlugin() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                new Metadata.Builder()
                        .name("Plugin")
                        .id("plugin-id")
                        .author("Author")
                        .version("1.0.0")
                        .license("MIT")
                        .type(PluginType.PLUGIN)
                        .entryPoints(List.of())
                        .build()
        );

        assertTrue(ex.getMessage().contains("entrypoints"));
    }

    @Test
    void build_shouldRequireEntrypoints_forProvider() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                new Metadata.Builder()
                        .name("Provider")
                        .id("provider-id")
                        .author("Author")
                        .version("1.0.0")
                        .license("MIT")
                        .type(PluginType.PROVIDER)
                        .entryPoints(List.of())
                        .build()
        );

        assertTrue(ex.getMessage().contains("entrypoints"));
    }

    @Test
    void build_shouldAllowEmptyEntrypoints_forLoaderAndLibrary() {
        Metadata loader = new Metadata.Builder()
                .name("Loader")
                .id("loader-id")
                .author("Author")
                .version("1.0.0")
                .license("MIT")
                .type(PluginType.LOADER)
                .entryPoints(List.of())
                .build();
    }

    @Test
    void build_shouldRejectMissingRequiredFields() {
        assertThrows(IllegalArgumentException.class, () -> new Metadata.Builder()
                .id("id")
                .author("author")
                .version("1")
                .license("lic")
                .type(PluginType.LOADER)
                .build());

        assertThrows(IllegalArgumentException.class, () -> new Metadata.Builder()
                .name("name")
                .author("author")
                .version("1")
                .license("lic")
                .type(PluginType.LOADER)
                .build());

        assertThrows(IllegalArgumentException.class, () -> new Metadata.Builder()
                .name("name")
                .id("id")
                .version("1")
                .license("lic")
                .type(PluginType.LOADER)
                .build());

        assertThrows(IllegalArgumentException.class, () -> new Metadata.Builder()
                .name("name")
                .id("id")
                .author("author")
                .license("lic")
                .type(PluginType.LOADER)
                .build());

        assertThrows(IllegalArgumentException.class, () -> new Metadata.Builder()
                .name("name")
                .id("id")
                .author("author")
                .version("1")
                .type(PluginType.LOADER)
                .build());
    }

    @Test
    void getPluginPath_shouldBeEmpty_whenPluginFileIsNull() {
        Metadata metadata = new Metadata.Builder()
                .name("Loader")
                .id("loader-id")
                .author("Author")
                .version("1.0.0")
                .license("MIT")
                .type(PluginType.LOADER)
                .entryPoints(List.of())
                .build();

        assertTrue(metadata.getPluginPath().isEmpty());
    }

    @Test
    void getPluginPath_shouldReturnPath_whenPluginFileIsSet() throws Exception {
        File tmp = File.createTempFile("metadata-test", ".jar");
        tmp.deleteOnExit();

        Metadata metadata = new Metadata.Builder()
                .name("Plugin")
                .id("plugin-id")
                .author("Author")
                .version("1.0.0")
                .license("MIT")
                .type(PluginType.PLUGIN)
                .entryPoints(List.of("com.example.Main"))
                .pluginFile(tmp)
                .build();

        Optional<Path> path = metadata.getPluginPath();
        assertTrue(path.isPresent());
        assertEquals(tmp.toPath(), path.get());
        assertEquals(tmp, metadata.getPluginFile());
    }

    @Test
    void toString_shouldContainKeyFields() {
        Metadata metadata = new Metadata.Builder()
                .name("Plugin")
                .id("plugin-id")
                .author("Author")
                .version("1.0.0")
                .license("MIT")
                .type(PluginType.PLUGIN)
                .entryPoints(List.of("com.example.Main"))
                .build();

        String s = metadata.toString();
        assertTrue(s.contains("plugin-id"));
        assertTrue(s.contains("Plugin"));
        assertTrue(s.contains("1.0.0"));
        assertTrue(s.contains("PLUGIN"));
    }
}