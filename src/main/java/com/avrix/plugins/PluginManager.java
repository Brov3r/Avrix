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
    void init();

    /**
     * Discovers and loads all available plugins.
     */
    void loadPlugins();

    /**
     * Loads and initializes a specific plugin.
     *
     * @param container the plugin data container
     */
    void loadPlugin(PluginData container);

    /**
     * Returns a map of all loaded plugins, keyed by their unique ID.
     *
     * @return map of plugin IDs to their data containers
     */
    Map<String, PluginData> getPlugins();

    /**
     * Resolves the correct loading order for plugins based on their dependencies.
     *
     * @param candidates the list of plugin metadata to sort
     * @return a sorted list representing the correct load order
     */
    List<Metadata> resolvePluginLoadOrder(List<Metadata> candidates);
}