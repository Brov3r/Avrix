package com.avrix.plugins;

import com.avrix.core.Bootstrap;
import com.avrix.core.Metadata;
import com.avrix.provider.GameProvider;

/**
 * Base class for all Avrix plugins.
 * Provides access to plugin metadata and defines the initialization lifecycle hook.
 * Extend this class to implement custom plugin logic and register with the framework.
 */
public abstract class Plugin {
    /**
     * Plugin metadata descriptor.
     */
    protected final Metadata metadata;

    /**
     * Creates a new {@link Plugin} instance.
     *
     * @param metadata {@link Plugin} {@link Metadata}, must not be {@code null}
     */
    public Plugin(Metadata metadata) {
        this.metadata = metadata;
    }

    /**
     * Returns the plugin's {@link Metadata}.
     *
     * @return immutable {@link Metadata} descriptor
     */
    public final Metadata getMetadata() {
        return metadata;
    }

    /**
     * Returns the main {@link GameProvider}.
     *
     * @return immutable {@link GameProvider} descriptor
     */
    public final GameProvider getProvider() {
        return Bootstrap.getGameProvider();
    }

    /**
     * Called by the framework when the plugin is ready for initialization.
     * Override to register commands, listeners, or perform startup tasks.
     */
    public abstract void onInitialize();
}
