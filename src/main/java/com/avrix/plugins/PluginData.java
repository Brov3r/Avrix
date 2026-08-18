package com.avrix.plugins;

import com.avrix.core.Metadata;

import java.io.File;
import java.net.URI;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable container holding runtime and descriptive information for a discovered or loaded plugin.
 * <p>
 * Encapsulates the plugin's metadata, physical file location, icon URI, and active runtime instance.
 * Core platform descriptors (game provider, loader) may lack physical files or instances.
 */
public final class PluginData {

    private final String id;
    private final File pluginFile;
    private final URI iconURI;
    private final Plugin instance;
    private final Metadata metadata;

    /**
     * Constructs a fully populated {@link PluginData} container.
     *
     * @param pluginFile the physical JAR file of the plugin, can be null for virtual/core plugins
     * @param iconURI    the URI pointing to the plugin icon, can be null
     * @param instance   the instantiated {@link Plugin} entrypoint object, can be null for pure libraries
     * @param metadata   the plugin metadata descriptor, cannot be null
     * @throws NullPointerException if {@code metadata} is null
     */
    public PluginData(File pluginFile, URI iconURI, Plugin instance, Metadata metadata) {
        this.metadata = Objects.requireNonNull(metadata, "Metadata cannot be null");
        this.id = metadata.id();
        this.pluginFile = pluginFile;
        this.iconURI = iconURI;
        this.instance = instance;
    }

    /**
     * Constructs a synthetic {@link PluginData} container containing only metadata.
     *
     * @param metadata the plugin metadata descriptor, cannot be null
     * @throws NullPointerException if {@code metadata} is null
     */
    public PluginData(Metadata metadata) {
        this(null, null, null, metadata);
    }

    /**
     * Returns the unique string identifier of the plugin.
     *
     * @return unique plugin ID
     */
    public String getId() {
        return id;
    }

    /**
     * Retrieves the physical JAR file of the plugin on disk, if present.
     *
     * @return optional containing the JAR file, or empty if virtual
     */
    public Optional<File> getPluginFile() {
        return Optional.ofNullable(pluginFile);
    }

    /**
     * Retrieves the physical {@link Path} of the plugin on disk, if present.
     *
     * @return optional containing the JAR path, or empty if virtual
     */
    public Optional<Path> getPluginPath() {
        return Optional.ofNullable(pluginFile).map(File::toPath);
    }

    /**
     * Retrieves the URI pointing to the plugin's embedded icon graphic, if present.
     *
     * @return optional containing the icon URI, or empty if not provided
     */
    public Optional<URI> getPluginIconURI() {
        return Optional.ofNullable(iconURI);
    }

    /**
     * Retrieves the initialized {@link Plugin} instance, if an entrypoint was declared.
     *
     * @return optional containing the active plugin instance, or empty if library-only
     */
    public Optional<Plugin> getPluginInstance() {
        return Optional.ofNullable(instance);
    }

    /**
     * Returns the declarative metadata descriptor of the plugin.
     *
     * @return non-null {@link Metadata} record
     */
    public Metadata getMetadata() {
        return metadata;
    }
}