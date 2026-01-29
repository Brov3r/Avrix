package com.avrix.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Set of main avrix constants
 */
public class Constants {
    /**
     * Folder name for plugins
     */
    public static final String PLUGINS_FOLDER_NAME = "plugins";

    /**
     * Plugin metadata file name
     */
    public static final String PLUGINS_METADATA_NAME = "metadata.yml";

    /**
     * Name of the default plugin config
     */
    public static final String PLUGINS_DEFAULT_CONFIG_NAME = "config.yml";

    /**
     * Extension for Jar archives
     */
    public static final String JAR_EXTENSION = ".jar";

    /**
     * Project version
     */
    public static final String LOADER_VERSION;

    /**
     * Project name
     */
    public static final String LOADER_NAME;

    /**
     * Project ID
     */
    public static final String LOADER_ID = "avrix-loader";

    /**
     * Project author
     */
    public static final String LOADER_AUTHOR = "Brov3r";

    /**
     * Project license
     */
    public static final String LOADER_LICENSE = "GNU GPLv3";

    /**
     * Project license
     */
    public static final String LOADER_CONTACTS = "https://github.com/Brov3r/Avrix";

    /*
     Initializing data and configuration file
    */
    static {
        Properties properties = new Properties();
        try (InputStream input = Constants.class.getClassLoader().getResourceAsStream("avrix.properties")) {
            if (input == null) {
                throw new IOException("Loader metadata file not found");
            }
            properties.load(input);

            LOADER_VERSION = properties.getProperty("version");
            LOADER_NAME = properties.getProperty("projectName");
        } catch (IOException e) {
            throw new RuntimeException("Failed to load configuration file", e);
        }
    }
}
