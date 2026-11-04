package com.avrix.provider;

import com.avrix.core.Metadata;

import java.util.List;

/**
 * Contract representing the metadata provider for the active Avrix loader runtime.
 */
public interface LoaderProvider {

    /**
     * Returns the unique string identifier of the loader (e.g., {@code "avrix-loader"}).
     *
     * @return loader identifier
     */
    public String getId();

    /**
     * Returns the human-readable display name of the loader.
     *
     * @return loader display name
     */
    public String getName();

    /**
     * Returns the semantic version of the loader runtime.
     *
     * @return SemVer version string
     */
    public String getVersion();

    /**
     * Returns the author attribution list of the loader project.
     *
     * @return unmodifiable list of author names
     */
    public List<String> getAuthors();

    /**
     * Returns the licensing model descriptor of the loader.
     *
     * @return license identifier string
     */
    public String getLicense();

    /**
     * Returns official contact and repository URLs for the loader.
     *
     * @return list of contact URL strings
     */
    public List<String> getContacts();

    /**
     * Synthesizes an immutable {@link Metadata} descriptor representing the loader.
     *
     * @return non-null {@link Metadata} record
     */
    public Metadata getMetadata();
}