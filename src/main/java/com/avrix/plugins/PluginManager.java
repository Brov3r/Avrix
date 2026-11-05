package com.avrix.plugins;

import com.avrix.core.Metadata;

import java.util.List;
import java.util.Map;

/**
 * Manages the discovery, dependency resolution, and loading of Avrix plugins.
 */
public interface PluginManager {

    /**
     * Initializes the plugin manager.
     */
    public void init();

    /**
     * Discovers and loads all available plugins.
     */
    public void loadPlugins();

    /**
     * Loads and initializes a specific plugin.
     * <p>
     * This method bypasses discovery-time approval. Callers are responsible for passing only trusted containers.
     *
     * @param container the plugin data container
     */
    public void loadPlugin(PluginData container);

    /**
     * Returns a map of all loaded plugins, keyed by their unique ID.
     *
     * @return map of plugin IDs to their data containers
     */
    public Map<String, PluginData> getPlugins();

    /**
     * Returns the results of the most recent plugin load attempt.
     *
     * @return immutable list of load results
     */
    public default List<PluginLoadResult> getLoadResults() {
        return List.of();
    }

    /**
     * Shuts down loaded plugins and releases plugin manager resources.
     */
    public default void close() {
    }

    /**
     * Resolves the correct loading order for plugins based on their dependencies.
     *
     * @param candidates the list of plugin metadata to sort
     * @return a sorted list representing the correct load order
     */
    public List<Metadata> resolvePluginLoadOrder(List<Metadata> candidates);
}