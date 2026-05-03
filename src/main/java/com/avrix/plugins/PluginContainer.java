package com.avrix.plugins;

import com.avrix.core.Metadata;

import java.io.File;
import java.net.URI;
import java.util.Objects;

/**
 * Immutable container holding a plugin's identity, physical files, and parsed metadata.
 *
 * @see Metadata
 */
public final class PluginContainer {
    private final String id;
    private final File pluginFile;
    private final URI iconURI;
    private final Plugin instance;
    private final Metadata metadata;

    /**
     * Creates a validated plugin container.
     *
     * @param pluginFile Main plugin artifact (e.g. {@code .jar}). May be {@code null}.
     * @param iconURI    Optional icon URI. May be {@code null}.
     * @param instance   Loaded plugin instance. May be {@code null} (e.g. for libraries).
     * @param metadata   Parsed plugin metadata. Must not be {@code null}.
     * @throws NullPointerException if {@code metadata} is {@code null}.
     */
    public PluginContainer(File pluginFile, URI iconURI, Plugin instance, Metadata metadata) {
        this.id = metadata.getId();
        this.pluginFile = pluginFile;
        this.iconURI = iconURI;
        this.instance = instance;
        this.metadata = Objects.requireNonNull(metadata, "Metadata cannot be null!");
    }

    /**
     * Convenience constructor for metadata-only containers.
     * Delegates to {@link #PluginContainer(File, URI, Plugin, Metadata)}.
     *
     * @param metadata Parsed plugin metadata. Must not be {@code null}.
     * @throws NullPointerException if {@code metadata} is {@code null}.
     */
    public PluginContainer(Metadata metadata) {
        this(null, null, null, metadata);
    }

    /**
     * @return Unique plugin identifier (never {@code null} or blank).
     */
    public String getId() {
        return id;
    }

    /**
     * @return Primary plugin {@link File}, or {@code null} if absent.
     */
    public File getPluginFile() {
        return pluginFile;
    }

    /**
     * @return Optional icon {@link URI}, or {@code null} if absent.
     */
    public URI getPluginIconURI() {
        return iconURI;
    }

    /**
     * @return Parsed plugin {@link Metadata} (never {@code null}).
     */
    public Metadata getMetadata() {
        return metadata;
    }

    /**
     * @return Instance of the loaded {@link Plugin}, or {@code null} if absent.
     */
    public Plugin getPluginInstance() {
        return instance;
    }
}
