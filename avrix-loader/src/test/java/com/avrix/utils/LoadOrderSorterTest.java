package com.avrix.utils;

import com.avrix.enums.PluginType;
import com.avrix.plugins.Metadata;
import org.junit.jupiter.api.Test;
import org.tinylog.Logger;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class LoadOrderSorterTest {
    @Test
    public void sortsByTierThenDependencies_happyPath() {
        Metadata loader = md("loader", PluginType.LOADER, deps());
        Metadata provider = md("provider", PluginType.PROVIDER, deps("loader"));
        Metadata plugin = md("plugin-a", PluginType.PLUGIN, deps("provider"));

        List<Metadata> sorted = LoadOrderSorter.sort(List.of(plugin, provider, loader));

        logCase("happyPath", List.of(plugin, provider, loader), sorted);
        assertEquals(List.of("loader", "provider", "plugin-a"), ids(sorted));
    }

    @Test
    public void sortsWithinPluginTier_topologically() {
        Metadata loader = md("loader", PluginType.LOADER, deps());
        Metadata provider = md("provider", PluginType.PROVIDER, deps("loader"));

        Metadata a = md("plugin-a", PluginType.PLUGIN, deps("provider"));
        Metadata b = md("plugin-b", PluginType.PLUGIN, deps("plugin-a"));
        Metadata c = md("plugin-c", PluginType.PLUGIN, deps("provider"));

        List<Metadata> sorted = LoadOrderSorter.sort(List.of(c, b, a, provider, loader));

        logCase("pluginTopo", List.of(c, b, a, provider, loader), sorted);
        assertEquals(List.of("loader", "provider", "plugin-a", "plugin-b", "plugin-c"), ids(sorted));
    }

    @Test
    public void missingDependency_throws() {
        Metadata loader = md("loader", PluginType.LOADER, deps());
        Metadata provider = md("provider", PluginType.PROVIDER, deps("loader"));
        Metadata plugin = md("plugin-a", PluginType.PLUGIN, deps("missing"));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> LoadOrderSorter.sort(List.of(loader, provider, plugin)));

        Logger.info("missingDependency: {}", ex.getMessage());
        assertTrue(ex.getMessage().contains("Missing dependency"));
    }

    @Test
    public void tierViolation_loaderDependsOnPlugin_throws() {
        Metadata plugin = md("plugin-a", PluginType.PLUGIN, deps());
        Metadata loader = md("loader", PluginType.LOADER, deps("plugin-a"));
        Metadata provider = md("provider", PluginType.PROVIDER, deps("loader"));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> LoadOrderSorter.sort(List.of(loader, provider, plugin)));

        Logger.info("tierViolation: {}", ex.getMessage());
        assertTrue(ex.getMessage().contains("Dependency tier violation"));
    }

    @Test
    public void cycleWithinPlugins_throws() {
        Metadata loader = md("loader", PluginType.LOADER, deps());
        Metadata provider = md("provider", PluginType.PROVIDER, deps("loader"));

        Metadata a = md("plugin-a", PluginType.PLUGIN, deps("plugin-b"));
        Metadata b = md("plugin-b", PluginType.PLUGIN, deps("plugin-a"));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> LoadOrderSorter.sort(List.of(loader, provider, a, b)));

        Logger.info("cycleWithinPlugins: {}", ex.getMessage());
        assertTrue(ex.getMessage().contains("Cyclic dependency"));
    }

    @Test
    public void multipleLoaders_throws() {
        Metadata loader1 = md("loader-1", PluginType.LOADER, deps());
        Metadata loader2 = md("loader-2", PluginType.LOADER, deps());
        Metadata provider = md("provider", PluginType.PROVIDER, deps("loader-1"));
        Metadata plugin = md("plugin-a", PluginType.PLUGIN, deps("provider"));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> LoadOrderSorter.sort(List.of(plugin, provider, loader1, loader2)));

        Logger.info("multipleLoaders: {}", ex.getMessage());
        assertTrue(ex.getMessage().contains("Exactly one LOADER"));
    }

    @Test
    public void multipleProviders_throws() {
        Metadata loader = md("loader", PluginType.LOADER, deps());
        Metadata provider1 = md("provider-1", PluginType.PROVIDER, deps("loader"));
        Metadata provider2 = md("provider-2", PluginType.PROVIDER, deps("loader"));
        Metadata plugin = md("plugin-a", PluginType.PLUGIN, deps("provider-1"));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> LoadOrderSorter.sort(List.of(plugin, provider1, provider2, loader)));

        Logger.info("multipleProviders: {}", ex.getMessage());
        assertTrue(ex.getMessage().contains("Exactly one PROVIDER"));
    }

    private static void logCase(String name, List<Metadata> input, List<Metadata> output) {
        Logger.info("=== {} ===", name);
        Logger.info("Input:");
        for (Metadata md : input) {
            Logger.info("  - {}:{} deps={}", md.getType(), md.getId(), md.getDependencies().keySet());
        }
        Logger.info("Output:");
        for (int i = 0; i < output.size(); i++) {
            Metadata md = output.get(i);
            Logger.info("  {}) {}:{}", i, md.getType(), md.getId());
        }
    }

    @Test
    public void complexGraph_manyPlugins_sortedCorrectly() {
        Metadata loader = md("loader", PluginType.LOADER, deps());
        Metadata provider = md("provider", PluginType.PROVIDER, deps("loader"));

        Metadata a = md("plugin-a", PluginType.PLUGIN, deps("provider"));
        Metadata b = md("plugin-b", PluginType.PLUGIN, deps("provider", "plugin-a", "plugin-f", "plugin-e"));
        Metadata c = md("plugin-c", PluginType.PLUGIN, deps("provider", "plugin-a"));
        Metadata d = md("plugin-d", PluginType.PLUGIN, deps("provider", "plugin-b", "plugin-c"));
        Metadata e = md("plugin-e", PluginType.PLUGIN, deps("provider"));
        Metadata f = md("plugin-f", PluginType.PLUGIN, deps("provider", "plugin-e"));
        Metadata g = md("plugin-g", PluginType.PLUGIN, deps("provider", "plugin-d", "plugin-f"));

        List<Metadata> sorted = LoadOrderSorter.sort(List.of(g, f, e, d, c, b, a, provider, loader));
        logCase("complexGraph_manyPlugins", List.of(g, f, e, d, c, b, a, provider, loader), sorted);

        List<String> order = ids(sorted);

        assertEquals("loader", order.get(0));
        assertEquals("provider", order.get(1));

        assertEquals(
                List.of("loader", "provider", "plugin-a", "plugin-c", "plugin-e", "plugin-f", "plugin-b", "plugin-d", "plugin-g"),
                order
        );

        // Additional safety: partial order checks (more readable failures if something changes)
        assertBefore(order, "plugin-a", "plugin-b");
        assertBefore(order, "plugin-a", "plugin-c");
        assertBefore(order, "plugin-b", "plugin-d");
        assertBefore(order, "plugin-c", "plugin-d");
        assertBefore(order, "plugin-e", "plugin-f");
        assertBefore(order, "plugin-d", "plugin-g");
        assertBefore(order, "plugin-f", "plugin-g");
    }

    @Test
    public void cycleInPlugins_complex_throws() {
        Metadata loader = md("loader", PluginType.LOADER, deps());
        Metadata provider = md("provider", PluginType.PROVIDER, deps("loader"));

        // Cycle: a -> b -> c -> a
        Metadata a = md("plugin-a", PluginType.PLUGIN, deps("provider", "plugin-b"));
        Metadata b = md("plugin-b", PluginType.PLUGIN, deps("provider", "plugin-c"));
        Metadata c = md("plugin-c", PluginType.PLUGIN, deps("provider", "plugin-a"));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> LoadOrderSorter.sort(List.of(loader, provider, a, b, c)));

        Logger.info("cycleInPlugins_complex: {}", ex.getMessage());
        assertTrue(ex.getMessage().contains("Cyclic dependency"));
    }

    private static void assertBefore(List<String> order, String first, String second) {
        int i = order.indexOf(first);
        int j = order.indexOf(second);
        assertTrue(i >= 0 && j >= 0, "Missing ids in order: " + first + ", " + second + " -> " + order);
        assertTrue(i < j, "Expected '" + first + "' before '" + second + "' but order was: " + order);
    }

    private static List<String> ids(List<Metadata> list) {
        return list.stream().map(Metadata::getId).toList();
    }

    private static Map<String, String> deps(String... ids) {
        if (ids.length == 0) {
            return Map.of();
        }
        Map<String, String> m = new LinkedHashMap<>();
        for (String id : ids) {
            m.put(id, "*");
        }
        return m;
    }

    private static Metadata md(String id, PluginType type, Map<String, String> deps) {
        return new Metadata.Builder()
                .name("Test " + id)
                .id(id)
                .author("Test")
                .version("1.0.0")
                .license("MIT")
                .type(type)
                .entryPoints(List.of("com.example.Entry"))
                .dependencies(deps)
                .pluginFile(null)
                .build();
    }
}
