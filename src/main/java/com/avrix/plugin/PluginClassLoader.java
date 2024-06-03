package com.avrix.plugin;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * A custom class loader for loading plugin classes.
 * Extends {@link URLClassLoader} to provide functionality for loading classes from a {@link URL}, such as a JAR file.
 * <p>This class loader is specifically designed to work with plugins, allowing classes to be dynamically loaded
 * and accessed during runtime. It also includes a method to check if a class has already been loaded, which helps
 * prevent duplicate loading of the same class.</p>
 */
public class PluginClassLoader extends URLClassLoader {
    private static final Map<String, Class<?>> classCache = new HashMap<>(); // Storage of all cached classes
    private static final Map<String, PluginClassLoader> pluginLoaders = new LinkedHashMap<>(); // Storage of all created loaders
    private final Map<String, Class<?>> classes = new HashMap<>(); // Map of loaded classes

    /**
     * Constructs a new {@link PluginClassLoader} for the specified {@link URL}s using the specified parent class loader.
     *
     * @param pluginId ID of the plugin being loaded, according to data from {@link Metadata}
     * @param urls     The {@link URL}s from which to load classes and resources.
     */
    public PluginClassLoader(String pluginId, URL[] urls) {
        super(urls);
        pluginLoaders.put(pluginId, this);
    }

    /**
     * Finds a loaded class by name.
     * This method is used to check if a class has already been loaded by this class loader.
     *
     * @param name The fully qualified name of the desired {@link Class}.
     * @return The {@link Class} object, or {@code null} if the class has not been loaded.
     */
    public Class<?> findLoaded(String name) {
        return super.findLoadedClass(name);
    }

    /**
     * Finds and loads the class with the specified name from the {@link URL} search
     * path. Any {@link URL}s referring to JAR files are loaded and opened as needed
     * until the class is found.
     *
     * @param name the name of the class
     * @return the resulting {@link Class}
     * @throws ClassNotFoundException if the class could not be found,
     *                                or if the loader is closed.
     * @throws NullPointerException   if {@code name} is {@code null}.
     */
    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        return findClass(name, true);
    }

    /**
     * Retrieves the {@link Class} object for a given class name from the cache or loaders.
     * If the class is not found in the cache, searches through all loaders to find the class.
     *
     * @param name The fully qualified name of the desired class.
     * @return The {@link Class} object corresponding to the specified name, or {@code null} if the class could not be found.
     */
    public static Class<?> getClassByName(final String name) {
        Class<?> cachedClass = classCache.get(name);

        if (cachedClass != null) {
            return cachedClass;
        } else {
            for (PluginClassLoader loader : pluginLoaders.values()) {
                try {
                    cachedClass = loader.findClass(name, false);
                } catch (ClassNotFoundException ignored) {
                }
                if (cachedClass != null) {
                    classCache.put(name, cachedClass);
                    return cachedClass;
                }
            }
        }
        return null;
    }

    /**
     * Finds a class by name, optionally performing a global search.
     *
     * @param name           The fully qualified name of the desired class.
     * @param isGlobalSearch Indicates whether to perform a global search for the class.
     *                       If true, attempts to retrieve the class from {@link PluginManager} if not found locally.
     * @return The {@link Class} object corresponding to the specified name.
     * @throws ClassNotFoundException If the class could not be found.
     */
    protected Class<?> findClass(String name, boolean isGlobalSearch) throws ClassNotFoundException {
        Class<?> result = classes.get(name);

        if (result == null) {
            if (isGlobalSearch) {
                result = getClassByName(name);
            }

            if (result == null) {
                result = super.findClass(name);
                classCache.putIfAbsent(name, result);
            }

            classes.put(name, result);
        }

        return result;
    }

    /**
     * Retrieves the set of loaded class names.
     *
     * @return A set containing the names of the loaded classes.
     */
    public Set<String> getClasses() {
        return classes.keySet();
    }
}