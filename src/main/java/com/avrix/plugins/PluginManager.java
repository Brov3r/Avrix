package com.avrix.plugins;

import com.avrix.core.BaseClassLoader;
import com.avrix.core.Metadata;

import java.util.List;
import java.util.Map;

/**
 * Manages plugin lifecycle: discovery, loading, dependency resolution, and runtime access.
 */
public interface PluginManager {
    /**
     * Initializes the plugin manager subsystem.
     * Must be called before any plugin operations.
     *
     * @param classLoader the loader for plugin manager
     */
    void init(BaseClassLoader classLoader);

    /**
     * Scans configured directories and loads all discovered plugins.
     * Plugins are validated, dependencies resolved, and containers registered.
     */
    void loadPlugins();

    /**
     * Loads a single plugin from the given file with pre-parsed metadata.
     *
     * @param container {@link PluginContainer} containing all the necessary information for the loading
     */
    void loadPlugin(PluginContainer container);

    /**
     * @return Immutable map of loaded plugins keyed by plugin ID.
     * Never {@code null}; may be empty if no plugins loaded.
     */
    Map<String, PluginContainer> getPlugins();

    /**
     * Resolves correct load order for plugins based on dependencies and soft/ hard requirements.
     *
     * @param candidates List of plugin metadata to sort.
     * @return Topologically sorted list respecting dependency graph.
     */
    List<Metadata> resolvePluginLoadOrder(List<Metadata> candidates);
}