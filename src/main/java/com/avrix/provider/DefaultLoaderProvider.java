package com.avrix.provider;

import com.avrix.api.LoaderProvider;
import com.avrix.core.Environment;
import com.avrix.core.Metadata;
import com.avrix.utils.Constants;
import com.avrix.utils.LoaderProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * Production-ready default implementation of {@link LoaderProvider}.
 * <p>
 * Reads optional {@code avrix.*} overrides from the packaged {@code avrix.properties} resource. The Gradle build
 * replaces the {@code version} and {@code projectName} tokens; the remaining values fall back to {@link Constants}
 * when no override is present. Missing or unreadable resources are handled gracefully.
 *
 * <p><b>API Note:</b> Instantiated and registered in {@code ServiceManager} during the bootstrap lifecycle phase.
 */
public final class DefaultLoaderProvider implements LoaderProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultLoaderProvider.class);

    private final String id;
    private final String name;
    private final String version;
    private final List<String> authors;
    private final String license;
    private final List<String> contacts;
    private final Metadata metadata;

    /**
     * Constructs and initializes the loader provider descriptor.
     * <p>
     * Attempts to read optional build-property overrides from {@code avrix.properties} on the classpath. If the
     * resource is unavailable or an override is absent, falls back to default constant values defined in
     * {@link Constants}.
     *
     * <p><b>Implementation Note:</b> Initializes an immutable synthetic {@link Metadata} instance representing the loader runtime.
     */
    public DefaultLoaderProvider() {
        Properties properties;
        try {
            properties = LoaderProperties.read(getClass().getClassLoader());
            if (properties == null) {
                LOGGER.debug(
                        "Resource [{}] not found on classpath. Falling back to default constants.",
                        LoaderProperties.RESOURCE_NAME
                );
                properties = new Properties();
            }
        } catch (IOException e) {
            LOGGER.warn(
                    "Failed to read [{}]. Using default loader constants.",
                    LoaderProperties.RESOURCE_NAME,
                    e
            );
            properties = new Properties();
        }

        this.id = properties.getProperty("avrix.id", Constants.LOADER_ID);
        this.name = properties.getProperty("avrix.name", Constants.LOADER_NAME);
        this.version = properties.getProperty("avrix.version", Constants.LOADER_VERSION);
        this.license = properties.getProperty("avrix.license", Constants.LOADER_LICENSE);

        String authorsProp = properties.getProperty("avrix.authors", String.join(",", Constants.LOADER_AUTHOR));
        this.authors = Arrays.stream(authorsProp.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();

        String contactsProp = properties.getProperty("avrix.contacts", String.join(",", Constants.LOADER_CONTACTS));
        this.contacts = Arrays.stream(contactsProp.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();

        this.metadata = new Metadata.Builder()
                .schema(Constants.METADATA_SCHEMA)
                .id(this.id)
                .name(this.name)
                .version(this.version)
                .environment(Environment.BOTH)
                .authors(this.authors)
                .license(this.license)
                .contacts(this.contacts)
                .build();
    }

    /**
     * Returns the unique string identifier of the Avrix loader.
     *
     * @return unique loader ID (e.g., {@code "avrix-loader"})
     */
    @Override
    public String getId() {
        return id;
    }

    /**
     * Returns the human-readable display name of the Avrix loader.
     *
     * @return loader display name
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * Returns the semantic version string of the loader runtime.
     *
     * @return SemVer version string (e.g., {@code "2.1.0"})
     */
    @Override
    public String getVersion() {
        return version;
    }

    /**
     * Returns the list of author attribution names for the loader project.
     *
     * @return unmodifiable list of author names
     */
    @Override
    public List<String> getAuthors() {
        return authors;
    }

    /**
     * Returns the software license identifier of the loader runtime.
     *
     * @return license identifier string (e.g., {@code "MIT"})
     */
    @Override
    public String getLicense() {
        return license;
    }

    /**
     * Returns the official contact and repository endpoints for the loader project.
     *
     * @return unmodifiable list of contact URL strings
     */
    @Override
    public List<String> getContacts() {
        return contacts;
    }

    /**
     * Returns the synthesized immutable metadata descriptor representing the loader.
     *
     * @return non-null {@link Metadata} record
     */
    @Override
    public Metadata getMetadata() {
        return metadata;
    }
}