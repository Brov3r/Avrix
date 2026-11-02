package com.avrix.plugins;

/**
 * Outcome of a plugin load attempt.
 */
public enum PluginLoadStatus {
    /**
     * Plugin was loaded successfully.
     */
    LOADED,

    /**
     * Plugin failed to load.
     */
    FAILED,

    /**
     * Plugin was skipped before loading.
     */
    SKIPPED
}
