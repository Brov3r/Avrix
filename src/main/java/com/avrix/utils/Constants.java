package com.avrix.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Set of main avrix constants
 */
public class Constants {
    /**
     * Name of the file cache directory
     */
    public static final String CACHE_DIR_NAME = "avrix/cache";

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
     * Project version
     */
    public static final String AVRIX_VERSION;

    /**
     * Project name
     */
    public static final String AVRIX_NAME;

    /**
     * Maximum cache folder size (in megabytes).
     * If exceeded, it will be cleared at the next start.
     */
    public static final int MAX_CACHE_SIZE = 256;

    /*
     Initializing data and configuration file
    */
    static {
        Properties properties = new Properties();
        try (InputStream input = Constants.class.getClassLoader().getResourceAsStream("metadata/avrix.properties")) {
            if (input == null) {
                throw new IOException("[!] Loader metadata file not found");
            }
            properties.load(input);

            AVRIX_VERSION = properties.getProperty("version");
            AVRIX_NAME = properties.getProperty("projectName");
        } catch (IOException e) {
            throw new RuntimeException("[!] Failed to load configuration file", e);
        }
    }
}