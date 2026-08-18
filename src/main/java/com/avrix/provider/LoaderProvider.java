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
    String getId();

    /**
     * Returns the human-readable display name of the loader.
     *
     * @return loader display name
     */
    String getName();

    /**
     * Returns the semantic version of the loader runtime.
     *
     * @return SemVer version string
     */
    String getVersion();

    /**
     * Returns the author attribution list of the loader project.
     *
     * @return unmodifiable list of author names
     */
    List<String> getAuthors();

    /**
     * Returns the licensing model descriptor of the loader.
     *
     * @return license identifier string
     */
    String getLicense();

    /**
     * Returns official contact and repository URLs for the loader.
     *
     * @return list of contact URL strings
     */
    List<String> getContacts();

    /**
     * Synthesizes an immutable {@link Metadata} descriptor representing the loader.
     *
     * @return non-null {@link Metadata} record
     */
    Metadata getMetadata();
}