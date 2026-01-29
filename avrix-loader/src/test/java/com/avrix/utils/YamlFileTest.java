package com.avrix.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class YamlFileTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldLoadFromInputStreamAndReadValues() {
        String yaml = """
                plugin:
                  id: test
                  enabled: true
                  timeout: 42
                  tags: [a, b]
                """;

        YamlFile file = new YamlFile(
                new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)),
                tempDir.resolve("in.yml"),
                "in.yml"
        );

        assertEquals("test", file.getString("plugin.id"));
        assertTrue(file.getBoolean("plugin.enabled"));
        assertEquals(42, file.getInt("plugin.timeout"));
        assertEquals(List.of("a", "b"), file.getStringList("plugin.tags"));
    }

    @Test
    void shouldSetAndRemoveValuesUsingDotPath() {
        YamlFile file = new YamlFile(new ByteArrayInputStream("{}".getBytes(StandardCharsets.UTF_8)),
                tempDir.resolve("a.yml"), "a.yml");

        file.setString("a.b.c", "x");
        assertEquals("x", file.getString("a.b.c"));

        file.remove("a.b.c");
        assertNull(file.getValue("a.b.c"));
    }

    @Test
    void shouldRejectInvalidDotPath() {
        YamlFile file = new YamlFile(new ByteArrayInputStream("{}".getBytes(StandardCharsets.UTF_8)),
                tempDir.resolve("a.yml"), "a.yml");

        assertThrows(IllegalArgumentException.class, () -> file.getValue("  "));
        assertThrows(IllegalArgumentException.class, () -> file.getValue("a..b"));
        assertThrows(IllegalArgumentException.class, () -> file.setValue(".a", 1));
        assertThrows(IllegalArgumentException.class, () -> file.remove("a."));
    }


    @Test
    void shouldMergeRecursively() {
        String base = """
                a:
                  b:
                    c: 1
                    d: 2
                """;
        String patch = """
                a:
                  b:
                    d: 999
                    e: 3
                """;

        YamlFile file = new YamlFile(new ByteArrayInputStream(base.getBytes(StandardCharsets.UTF_8)),
                tempDir.resolve("m.yml"), "m.yml");
        YamlFile patchFile = new YamlFile(new ByteArrayInputStream(patch.getBytes(StandardCharsets.UTF_8)),
                tempDir.resolve("p.yml"), "p.yml");

        file.merge(patchFile);

        assertEquals(1, file.getInt("a.b.c"));
        assertEquals(999, file.getInt("a.b.d"));
        assertEquals(3, file.getInt("a.b.e"));
    }

    @Test
    void getMapShouldReturnCopyAndNeverNull() {
        String yaml = """
                map:
                  k1: v1
                  k2: v2
                """;
        YamlFile file = new YamlFile(new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)),
                tempDir.resolve("m.yml"), "m.yml");

        Map<String, Object> map = file.getMap("map");
        assertEquals(Map.of("k1", "v1", "k2", "v2"), map);

        // Defensive copy: external modifications must not affect internal state.
        map.put("k3", "v3");
        assertNull(file.getValue("map.k3"));
    }

    @Test
    void shouldSaveAndReload() throws Exception {
        String yaml = """
                a:
                  b: 1
                """;
        Path out = tempDir.resolve("save.yml");

        YamlFile file = new YamlFile(new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)), out, "save.yml");
        file.setInt("a.c", 2);
        file.save(out.toString());

        YamlFile reloaded = new YamlFile(out);
        assertEquals(1, reloaded.getInt("a.b"));
        assertEquals(2, reloaded.getInt("a.c"));
    }
}
