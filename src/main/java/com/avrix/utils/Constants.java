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
     * Project version
     */
    public static final String AVRIX_VERSION;

    /**
     * Project name
     */
    public static final String AVRIX_NAME;

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