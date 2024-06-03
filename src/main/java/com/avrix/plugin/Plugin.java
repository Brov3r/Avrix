package com.avrix.plugin;

/**
 * Basic template for implementing the plugin entry point.
 */
public abstract class Plugin {
    private final Metadata metadata; // Plugin metadata

    /**
     * Constructs a new {@link Plugin} with the specified metadata.
     * {@link Metadata} is transferred when the plugin is loaded into the game context.
     *
     * @param metadata The {@link Metadata} associated with this plugin.
     */
    public Plugin(Metadata metadata) {
        this.metadata = metadata;
    }

    /**
     * Returns the {@link Metadata} associated with this plugin.
     *
     * @return The {@link Metadata} of the plugin.
     */
    public Metadata getMetadata() {
        return this.metadata;
    }

    /**
     * Called when the plugin is initialized.
     * <p>
     * Implementing classes should override this method to provide the initialization logic.
     */
    public abstract void onInitialize();
}
