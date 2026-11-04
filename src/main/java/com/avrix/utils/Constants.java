package com.avrix.utils;

import java.io.IOException;
import java.util.Objects;
import java.util.Properties;

/**
 * Global constant definitions and runtime metadata for the Avrix loader.
 *
 * <p><b>API Note:</b> This is a static utility class and cannot be instantiated.
 */
public final class Constants {

    /**
     * CLI argument flag used to disable console output redirection to log files.
     */
    public static final String NO_REDIRECT_FLAG_NAME = "--no-redirect-log";

    /**
     * CLI argument flag that makes plugin load failures abort startup.
     */
    public static final String STRICT_FLAG_NAME = "--strict";

    /**
     * Standard manifest file name containing plugin metadata.
     */
    public static final String METADATA_NAME = "metadata.yml";

    /**
     * Default icon file name for plugins.
     */
    public static final String PLUGIN_ICON_NAME = "icon.png";

    /**
     * Supported metadata schema specification version.
     */
    public static final int METADATA_SCHEMA = 1;

    /**
     * File extension for plugin archive packages.
     */
    public static final String PLUGIN_EXTENSION = ".jar";

    /**
     * Name of the root directory where plugins are discovered and loaded.
     */
    public static final String PLUGINS_FOLDER_NAME = "plugins";

    /**
     * File name containing plugin identifiers or archive names that must not be loaded.
     */
    public static final String DISABLED_PLUGINS_FILE_NAME = "disabled.txt";

    /**
     * Loader runtime version resolved from build properties.
     */
    public static final String LOADER_VERSION;

    /**
     * Human-readable name of the loader runtime.
     */
    public static final String LOADER_NAME;

    /**
     * Unique identifier for the loader subsystem.
     */
    public static final String LOADER_ID = "avrix-loader";

    /**
     * Identifier of the default game provider.
     */
    public static final String DEFAULT_GAME_PROVIDER_ID = "project-zomboid";

    /**
     * System property used to select a game provider by identifier.
     */
    public static final String GAME_PROVIDER_PROPERTY = "avrix.provider";

    /**
     * Primary author / maintainer of the project.
     */
    public static final String LOADER_AUTHOR = "Brov3r";

    /**
     * Project distribution license identifier.
     */
    public static final String LOADER_LICENSE = "MIT";

    /**
     * Official repository and contact URL.
     */
    public static final String LOADER_CONTACTS = "https://github.com/Brov3r/Avrix";

    static {
        try {
            Properties props = LoaderProperties.read(Constants.class.getClassLoader());
            if (props == null) {
                throw new IllegalStateException("Resource '" + LoaderProperties.RESOURCE_NAME + "' not found in classpath");
            }

            LOADER_VERSION = Objects.requireNonNull(
                    props.getProperty("version"),
                    "Missing 'version' in " + LoaderProperties.RESOURCE_NAME
            );
            LOADER_NAME = Objects.requireNonNull(
                    props.getProperty("projectName"),
                    "Missing 'projectName' in " + LoaderProperties.RESOURCE_NAME
            );
        } catch (IOException | IllegalStateException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private Constants() {
        throw new UnsupportedOperationException("Constants utility class cannot be instantiated");
    }
}