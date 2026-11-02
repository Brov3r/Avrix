package com.avrix.plugins;

/**
 * Represents a base plugin interface.
 * <p>
 * Implementations of this interface are loaded and executed by the plugin system.
 */
public interface Plugin {

    /**
     * Called when the plugin is initialized.
     *
     * @param pluginData plugin information
     */
    void onInitialize(PluginData pluginData);

    /**
     * Called when the loader is shutting down.
     * <p>
     * Implementations should release plugin-owned resources here. The default implementation does nothing.
     */
    default void onShutdown() {
    }
}