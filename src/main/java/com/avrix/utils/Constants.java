package com.avrix.utils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Properties;

/**
 * Set of main avrix constants
 */
public class Constants {
    /**
     * Name of the flag to disable log redirection
     */
    public static final String NO_REDIRECT_FLAG_NAME = "-no-redirect-log";

    /**
     * File name for metadata
     */
    public static final String METADATA_NAME = "metadata.yml";

    /**
     * File name for plugin icon
     */
    public static final String PLUGINS_ICON_NAME = "icon.png";

    /**
     * Metadata schema version
     */
    public static final int METADATA_SCHEMA = 1;

    /**
     * Extension for Plugin archives
     */
    public static final String PLUGIN_EXTENSION = ".jar";

    /**
     * Name of the directory with plugins
     */
    public static final String PLUGINS_FOLDER_NAME = "plugins";

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
        try (var in = Constants.class.getClassLoader().getResourceAsStream("avrix.properties")) {
            if (in == null) throw new ExceptionInInitializerError("File 'avrix.properties' not found in classpath");
            var props = new Properties();
            props.load(new InputStreamReader(in, StandardCharsets.UTF_8));
            LOADER_VERSION = Objects.requireNonNull(props.getProperty("version"), "Missing 'version'");
            LOADER_NAME = Objects.requireNonNull(props.getProperty("projectName"), "Missing 'projectName'");
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }
}
