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
    public void onInitialize(PluginData pluginData);

    /**
     * Called when the loader is shutting down.
     * <p>
     * Also called when {@link #onInitialize(PluginData)} fails after the instance was constructed. Implementations
     * must tolerate partial initialization and release any resources acquired so far. The default does nothing.
     */
    public default void onShutdown() {
    }
}
