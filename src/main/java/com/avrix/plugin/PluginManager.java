package com.avrix.plugin;

import com.avrix.utils.Constants;
import com.avrix.utils.PatchManager;
import zombie.core.Core;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The PluginManager class manages the loading, initialization, and handling of plugins within the application context.
 */
public class PluginManager {
    private static final List<Metadata> pluginsList = new ArrayList<>(); // A list containing metadata for loaded plugins.

    /**
     * Prints information about loaded plugins to the console.
     * The information includes plugin names, IDs, and versions.
     */
    public static void printLoadedPluginsInfo() {
        StringBuilder sb = new StringBuilder("Loaded plugins:\n");
        for (Metadata plugin : pluginsList) {
            sb.append("    - ").append(plugin.getName())
                    .append(" (ID: ").append(plugin.getId())
                    .append(", Version: ").append(plugin.getVersion())
                    .append(")\n");
        }
        System.out.print(sb);
    }

    /**
     * Loads default modules and adds them to the list of loaded plugins.
     */
    private static void loadDefaultModules() {
        pluginsList.add(new Metadata.MetadataBuilder()
                .name("Project Zomboid")
                .id("pz-core")
                .author("The Indie Stone")
                .environment("both")
                .version(Core.getInstance().getVersion())
                .license("Complex")
                .entryPointsList(Collections.emptyList()).build());

        pluginsList.add(new Metadata.MetadataBuilder()
                .name(Constants.AVRIX_NAME)
                .id("avrix-loader")
                .author("Brov3r")
                .environment("both")
                .version(Constants.AVRIX_VERSION)
                .license("GNU GPLv3")
                .contacts("https://github.com/Brov3r/Avrix")
                .entryPointsList(Collections.emptyList()).build());
    }

    /**
     * Loading plugins into the game context
     *
     * @throws Exception in case of any problems
     */
    public static void loadPlugins() throws Exception {
        // Plugin loading mode (client, server)
        PluginEnvironment loaderEnvironment = PluginEnvironment.fromString(System.getProperty("avrix.mode"));

        // Adding default modules (game, loader, etc.) to the list of plugins
        loadDefaultModules();

        // Getting valid plugins
        for (File plugin : getPluginFiles()) {
            Metadata metadata = Metadata.createFromJar(plugin, Constants.PLUGINS_METADATA_NAME);

            if (metadata == null) {
                System.out.printf("[!] No metadata found for potential plugin '%s'. Skipping...%n", plugin.getName());
                continue;
            }

            if (!metadata.getPluginFile().exists()) {
                System.out.printf("[!] Could not access plugin file '%s'. Skipping...%n", plugin.getName());
                continue;
            }

            // creating a folder for configs
            File configFolder = metadata.getConfigFolder();
            if (!configFolder.exists()) {
                try {
                    configFolder.mkdir();
                } catch (Exception e) {
                    System.out.printf("[!] An error occurred while creating the config folder for plugin '%s': %s%n", metadata.getId(), e.getMessage());
                }
            }

            pluginsList.add(metadata);
        }

        // Loading the plugin
        for (Metadata metadata : Metadata.sortMetadata(pluginsList)) {
            File pluginFile = metadata.getPluginFile();

            if (pluginFile == null) continue;

            PluginEnvironment environment = metadata.getEnvironment();

            System.out.printf("[#] Loading plugin '%s' (ID: %s, Version: %s)...%n", metadata.getName(), metadata.getId(), metadata.getVersion());

            // Checking the environment
            if (environment != loaderEnvironment && environment != PluginEnvironment.BOTH) {
                continue;
            }

            // Creating a URL for the plugin
            URL pluginUrl = pluginFile.toURI().toURL();

            PluginClassLoader classLoader = new PluginClassLoader(metadata.getId(), new URL[]{pluginUrl});

            // Applying patches
            PatchManager.applyPluginPatches(metadata, classLoader);

            // Loading the plugin
            loadPlugin(metadata, classLoader);
        }

        // Displaying information about loaded plugins
        printLoadedPluginsInfo();
    }

    /**
     * Loads and initializes plugin entry points.
     *
     * @param metadata    The {@link Metadata} of the plugin.
     * @param classLoader The {@link PluginClassLoader} to use for loading the plugin classes.
     * @throws ClassNotFoundException    If a specified class cannot be found.
     * @throws NoSuchMethodException     If the constructor with Metadata parameter is not found.
     * @throws InvocationTargetException If the underlying constructor throws an exception.
     * @throws InstantiationException    If the class that declares the underlying constructor represents an abstract class.
     * @throws IllegalAccessException    If the constructor is inaccessible.
     */
    public static void loadPlugin(Metadata metadata, ClassLoader classLoader) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        if (metadata.getEntryPoints() == null || metadata.getEntryPoints().isEmpty()) {
            return;
        }

        for (String entryPoint : metadata.getEntryPoints()) {
            Class<?> pluginClass = Class.forName(entryPoint, true, classLoader);
            Plugin pluginInstance = (Plugin) pluginClass.getDeclaredConstructor(Metadata.class).newInstance(metadata);
            pluginInstance.onInitialize();
        }
    }

    /**
     * Finds all JAR plugins in the specified directory. Also checks for the presence of a folder and
     * creates it if it is missing.
     *
     * @return List of JAR files.
     * @throws IOException in cases of input/output problems
     */
    private static List<File> getPluginFiles() throws IOException {
        File folder = new File(Constants.PLUGINS_FOLDER_NAME);

        if (!folder.exists()) {
            if (!folder.mkdir()) {
                System.out.println("[!] Failed to create plugins folder...");
            }
        }

        if (!folder.isDirectory()) {
            throw new IOException("[!] Path '" + folder.getPath() + "' is not a directory. Remove and try again!");
        }

        ArrayList<File> jarFiles = new ArrayList<>();

        File[] files = folder.listFiles((File pathname) -> pathname.isFile() && pathname.getName().endsWith(".jar"));

        if (files != null) {
            Collections.addAll(jarFiles, files);
        }

        return jarFiles;
    }
}