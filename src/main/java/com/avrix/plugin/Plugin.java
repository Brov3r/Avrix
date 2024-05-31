package com.avrix.plugin;

/**
 * Basic template for implementing the plugin entry point.
 */
public abstract class Plugin {
    /**
     * Called when the plugin is initialized.
     * <p>
     * Implementing classes should override this method to provide the initialization logic.
     */
    public abstract void onInitialize();
}
