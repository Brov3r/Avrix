package com.avrix.plugin;

import com.avrix.utils.Constants;
import com.avrix.utils.YamlFile;

import java.io.*;

/**
 * Basic template for implementing the plugin entry point.
 */
public abstract class Plugin {
    private YamlFile config; // Default plugin configuration file
    private final Metadata metadata; // Plugin metadata

    /**
     * Constructs a new {@link Plugin} with the specified metadata.
     * {@link Metadata} is transferred when the plugin is loaded into the game context.
     *
     * @param metadata The {@link Metadata} associated with this plugin.
     */
    public Plugin(Metadata metadata) {
        this.metadata = metadata;
    }

    /**
     * Returns the default configuration file.
     *
     * @return a {@link YamlFile} object, or {@code null}
     */
    public final synchronized YamlFile getDefaultConfig() {
        return config;
    }

    /**
     * Returns a {@link File} object representing the configuration directory for this plugin.
     * The directory path is normalized to prevent problems with various file systems.
     *
     * @return A {@link File} object pointing to the normalized path to the plugin configuration directory.
     */
    public final File getConfigFolder() {
        return metadata.getConfigFolder();
    }

    /**
     * Returns the {@link Metadata} associated with this plugin.
     *
     * @return The {@link Metadata} of the plugin.
     */
    public final Metadata getMetadata() {
        return this.metadata;
    }

    /**
     * Loading the default configuration file.
     * If there is no config in the plugin resources folder, it is saved from the Jar archive and then loaded.
     * <p>
     * If the configuration file cannot be loaded (for example, it is not in the Jar folder), then a new empty config is created.
     */
    public final synchronized void loadDefaultConfig() {
        File configFromFolder = getConfigFolder().toPath().resolve(Constants.PLUGINS_DEFAULT_CONFIG_NAME).toFile();
        try {
            if (!configFromFolder.exists()) {
                copyConfigFromJar(Constants.PLUGINS_DEFAULT_CONFIG_NAME, configFromFolder);
            }
            config = new YamlFile(configFromFolder);
        } catch (Exception e) {
            config = YamlFile.create(configFromFolder);
        }
    }

    /**
     * Copies a configuration file from the jar archive to the specified destination.
     *
     * @param fileName    the name of the configuration file to be copied
     * @param destination the destination file where the configuration will be copied
     */
    private void copyConfigFromJar(String fileName, File destination) {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(fileName);
             FileOutputStream out = new FileOutputStream(destination)) {

            if (in == null) {
                throw new FileNotFoundException("[!] Resource not found: " + fileName);
            }

            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        } catch (IOException e) {
            System.out.printf("[!] Error when copying config '%s' from plugin Jar archive: '%s'. Error: %s%n",
                    fileName, getClass().getSimpleName(), e.getMessage());
        }
    }

    /**
     * Loading the configuration file.
     * If there is no config in the plugin resources folder, it is saved from the Jar archive and then loaded.
     * <p>
     * If the configuration file cannot be loaded (for example, it is not in the Jar folder), then a new empty config is created.
     *
     * @param configPath path to the configuration file, relative to the plugin resources folder/root folder inside the Jar
     * @return returns a {@link YamlFile} object, which is a configuration file.
     */
    public final synchronized YamlFile loadConfig(String configPath) {
        File configFromFolder = getConfigFolder().toPath().resolve(configPath).toFile();

        try {
            File parentDir = configFromFolder.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                if (!parentDir.mkdirs()) {
                    throw new IOException("[!] Failed to create directories for: " + configFromFolder.getAbsolutePath());
                }
            }

            configPath = configPath.trim();

            if (!configPath.endsWith(".yml")) {
                configPath += ".yml";
            }

            if (!configFromFolder.exists()) {
                copyConfigFromJar(configPath, configFromFolder);
            }

            return new YamlFile(configFromFolder);
        } catch (Exception e) {
            return YamlFile.create(configFromFolder);
        }
    }

    /**
     * Called when the plugin is initialized.
     * <p>
     * Implementing classes should override this method to provide the initialization logic.
     */
    public abstract void onInitialize();
}