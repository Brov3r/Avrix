package com.avrix.plugin;

import com.avrix.enums.PluginEnvironment;
import com.avrix.utils.Constants;
import com.avrix.utils.PatchManager;
import com.avrix.utils.VersionChecker;
import zombie.core.Core;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.*;

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
                    .append(" (").append(plugin.getId())
                    .append(", ").append(plugin.getVersion())
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

        // List of all plugins with valid metadata
        Map<File, Metadata> validPlugins = new HashMap<>();

        for (File plugin : getPluginFiles()) {
            Metadata metadata = Metadata.createFromJar(plugin, Constants.PLUGINS_METADATA_NAME);

            if (metadata == null) {
                System.out.printf("[!] No metadata found for potential plugin '%s'. Skipping...%n", plugin.getName());
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

            validPlugins.put(plugin, metadata);
            pluginsList.add(metadata);
        }

        // Checking for dependency availability and versions
        dependencyVerification();

        // Sorting plugins by load order and Circular dependency check
        List<File> sortedOrder = sortPluginsByLoadOrder(validPlugins);

        // Loading the plugin
        for (File plugin : sortedOrder) {
            Metadata metadata = validPlugins.get(plugin);
            PluginEnvironment environment = metadata.getEnvironment();

            System.out.printf("[#] Loading plugin '%s'...%n", metadata.getName());

            // Checking the environment
            if (environment != loaderEnvironment && environment != PluginEnvironment.BOTH) {
                continue;
            }

            List<String> entryPoints = metadata.getEntryPoints();
            List<String> patchList = metadata.getPatchList();

            // Creating a URL for the plugin
            URL pluginUrl = plugin.toURI().toURL();

            PluginClassLoader classLoader = new PluginClassLoader(metadata.getId(), new URL[]{pluginUrl});

            // Applying patches
            PatchManager.applyPluginPatches(patchList, classLoader);

            // Loading the plugin
            loadPlugin(entryPoints, metadata, classLoader);
        }

        // Displaying information about loaded plugins
        printLoadedPluginsInfo();
    }

    /**
     * Loads and initializes plugin entry points.
     *
     * @param entryPoints A list of fully qualified class names of the plugin entry points.
     * @param metadata    The {@link Metadata} of the plugin.
     * @param classLoader The {@link PluginClassLoader} to use for loading the plugin classes.
     * @throws ClassNotFoundException    If a specified class cannot be found.
     * @throws NoSuchMethodException     If the constructor with Metadata parameter is not found.
     * @throws InvocationTargetException If the underlying constructor throws an exception.
     * @throws InstantiationException    If the class that declares the underlying constructor represents an abstract class.
     * @throws IllegalAccessException    If the constructor is inaccessible.
     */
    public static void loadPlugin(List<String> entryPoints, Metadata metadata, ClassLoader classLoader) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        if (entryPoints == null || entryPoints.isEmpty()) {
            return;
        }

        for (String entryPoint : entryPoints) {
            Class<?> pluginClass = Class.forName(entryPoint, true, classLoader);
            Plugin pluginInstance = (Plugin) pluginClass.getDeclaredConstructor(Metadata.class).newInstance(metadata);
            pluginInstance.onInitialize();
        }
    }

    /**
     * Sorts the plugins by their load order based on their dependencies.
     *
     * @param pluginsMap A {@link Map} containing plugin files and their associated metadata.
     * @return A list of plugin files sorted by their load order.
     * @throws Exception If a cyclic dependency is detected during sorting.
     */
    private static List<File> sortPluginsByLoadOrder(Map<File, Metadata> pluginsMap) throws Exception {
        Map<String, List<String>> dependencyGraph = new HashMap<>();
        Map<String, Integer> visitState = new HashMap<>();
        List<String> sortedOrder = new ArrayList<>();
        Map<String, File> pluginFilesMap = new HashMap<>();

        // Initializing the dependency and state graph
        for (Map.Entry<File, Metadata> entry : pluginsMap.entrySet()) {
            String pluginId = entry.getValue().getId();
            File pluginFile = entry.getKey();

            pluginFilesMap.put(pluginId, pluginFile);
            visitState.put(pluginId, 0); // 0 - not visited, 1 - in progress, 2 - visited

            dependencyGraph.putIfAbsent(pluginId, new ArrayList<>());

            if (entry.getValue().getDependencies() != null) continue;

            for (String dependency : entry.getValue().getDependencies().keySet()) {
                dependencyGraph.putIfAbsent(dependency, new ArrayList<>());
                dependencyGraph.get(dependency).add(pluginId);
            }
        }

        // Topological sorting and circular dependency checking
        for (String pluginId : dependencyGraph.keySet()) {
            if (visitState.get(pluginId) == 0 && hasCyclicDependency(pluginId, dependencyGraph, visitState, sortedOrder)) {
                throw new Exception("[!] Cyclic dependency detected: " + pluginId);
            }
        }

        // Convert to file list
        List<File> sortedPluginFiles = new ArrayList<>();
        for (String pluginId : sortedOrder) {
            sortedPluginFiles.add(pluginFilesMap.get(pluginId));
        }

        return sortedPluginFiles;
    }

    /**
     * Checks for cyclic dependencies in the plugin dependency graph using depth-first search.
     *
     * @param current     The current plugin being processed.
     * @param graph       The dependency graph of plugins.
     * @param state       The visit state of plugins.
     * @param sortedOrder The sorted order of plugins.
     * @return {@code true} if a cyclic dependency is found, {@code false} otherwise.
     */
    private static boolean hasCyclicDependency(String current, Map<String, List<String>> graph, Map<String, Integer> state, List<String> sortedOrder) {
        if (state.get(current) == 1) {
            return true; // Cyclic dependency detected
        }
        if (state.get(current) == 2) {
            return false; // Already visited
        }

        state.put(current, 1); // Mark as in progress
        for (String neighbor : graph.getOrDefault(current, Collections.emptyList())) {
            if (hasCyclicDependency(neighbor, graph, state, sortedOrder)) {
                return true;
            }
        }
        state.put(current, 2); // Mark as visited
        sortedOrder.add(0, current);

        return false;
    }

    /**
     * Checking plugins for their dependencies and ensuring that their versions meet the requirements
     *
     * @throws Exception in case the dependent plugin is not found among those found or its version does not meet the requirements
     */
    private static void dependencyVerification() throws Exception {
        // Creating a map for quick search by plugin ID
        Map<String, String> pluginMap = new HashMap<>();
        for (Metadata metadata : pluginsList) {
            pluginMap.put(metadata.getId(), metadata.getVersion());
        }

        // Checking the dependencies of each plugin
        for (Metadata metadata : pluginsList) {
            Map<String, String> dependencies = metadata.getDependencies();

            if (dependencies == null) continue;

            for (Map.Entry<String, String> depEntry : dependencies.entrySet()) {
                String depId = depEntry.getKey();
                String depVersion = depEntry.getValue();

                // Checking the availability of the plugin and version compatibility
                String version = pluginMap.get(depId);
                if (version == null || !VersionChecker.isVersionCompatible(version, depVersion)) {
                    throw new Exception(String.format("[!] Plugin '%s' does not have a dependent module '%s' or its version does not meet the requirements!",
                            metadata.getId(),
                            depId
                    ));
                }
            }
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