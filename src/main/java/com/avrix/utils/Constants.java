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
     * Runtime marker file used by Project Zomboid Lua code to detect an active Avrix launch.
     */
    public static final String RUNTIME_MARKER_NAME = "avrix-active.marker";

    /**
     * Project Zomboid mod.info key listing plugin archives to load from an active mod.
     */
    public static final String JAR_PLUGINS_KEY = "jarplugins";

    /**
     * Steam application identifier used by Project Zomboid Workshop content.
     */
    public static final String WORKSHOP_APP_ID = "108600";

    /**
     * File name containing the list of active client mods.
     */
    public static final String ACTIVE_MODS_FILE_NAME = "default.txt";

    /**
     * System property containing the active dedicated-server name.
     */
    public static final String SERVER_NAME_PROPERTY = "avrix.server.name";

    /**
     * Default dedicated-server name used when no {@code -servername} argument is present.
     */
    public static final String DEFAULT_SERVER_NAME = "servertest";

    /**
     * System property selecting the Workshop plugin approval policy.
     */
    public static final String PLUGIN_POLICY_PROPERTY = "avrix.plugin.policy";

    /**
     * CLI argument prefix used by Project Zomboid to override the user cache directory.
     */
    public static final String CACHE_DIR_ARGUMENT_PREFIX = "-cachedir=";

    /**
     * System property carrying the cache directory override parsed from {@link #CACHE_DIR_ARGUMENT_PREFIX}.
     */
    public static final String CACHE_DIR_PROPERTY = "avrix.cachedir";

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