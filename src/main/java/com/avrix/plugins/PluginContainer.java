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
    private final Metadata metadata;

    /**
     * Creates a validated plugin container.
     *
     * @param id         Unique plugin identifier. Must not be {@code null} or blank.
     * @param pluginFile Main plugin artifact (e.g. {@code .jar}). Must not be {@code null}.
     * @param iconURI    Optional icon URI. May be {@code null}.
     * @param metadata   Parsed plugin metadata. Must not be {@code null}.
     * @throws NullPointerException     if {@code id}, {@code pluginFile} or {@code metadata} is {@code null}.
     * @throws IllegalArgumentException if {@code id} is blank.
     */
    public PluginContainer(String id, File pluginFile, URI iconURI, Metadata metadata) {
        this.id = Objects.requireNonNull(id, "id");
        this.pluginFile = Objects.requireNonNull(pluginFile, "pluginFile");
        this.iconURI = iconURI;
        this.metadata = Objects.requireNonNull(metadata, "metadata");

        if (id.isBlank()) {
            throw new IllegalArgumentException("id cannot be blank");
        }
    }

    /**
     * @return Unique plugin identifier (never {@code null} or blank).
     */
    public String getId() {
        return id;
    }

    /**
     * @return Primary plugin file (never {@code null}).
     */
    public File getPluginFile() {
        return pluginFile;
    }

    /**
     * @return Optional icon URI, or {@code null} if absent.
     */
    public URI getPluginIconURI() {
        return iconURI;
    }

    /**
     * @return Parsed plugin metadata (never {@code null}).
     */
    public Metadata getMetadata() {
        return metadata;
    }
}
