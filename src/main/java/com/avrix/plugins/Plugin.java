package com.avrix.plugins;

import com.avrix.core.Metadata;

import java.io.File;
import java.net.URI;

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
     * Plugin jar file
     */
    protected final File pluginFile;

    /**
     * Plugin icon file URI. May be {@code null}.
     */
    protected final URI iconURI;

    /**
     * Plugin constructor, arguments are passed dynamically
     *
     * @param metadata   plugin {@link Metadata}
     * @param pluginFile plugin jar file
     * @param iconURI    plugin icon URI. May be {@code null}.
     */
    public Plugin(Metadata metadata, File pluginFile, URI iconURI) {
        this.metadata = metadata;
        this.pluginFile = pluginFile;
        this.iconURI = iconURI;
    }

    /**
     * Returns the jar plugin file reference.
     *
     * @return plugin jar {@link File}
     */
    public final File getPluginFile() {
        return pluginFile;
    }

    /**
     * Returns the jar plugin icon URI reference.
     *
     * @return plugin icon {@link URI}. May be {@code null}.
     */
    public final URI getIconURI() {
        return iconURI;
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
     * Called by the framework when the plugin is ready for initialization.
     * Override to register commands, listeners, or perform startup tasks.
     */
    public abstract void onInitialize();
}
