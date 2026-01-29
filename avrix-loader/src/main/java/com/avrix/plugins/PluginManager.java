package com.avrix.plugins;

import com.avrix.enums.PluginType;
import com.avrix.loaders.AvrixClassLoader;
import com.avrix.utils.Constants;
import com.avrix.utils.LoadOrderSorter;
import com.avrix.utils.VersionChecker;
import org.tinylog.Logger;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Discovers, sorts, initializes and launches Avrix plugins.
 */
public final class PluginManager {
    private static final Object LOCK = new Object();

    private static volatile boolean initialized;
    private static volatile boolean launched;

    private static volatile Metadata registeredLoader;
    private static volatile Metadata registeredProvider;

    private static List<Metadata> sortedMetadata = List.of();
    private static List<Plugin> loadedPlugins = List.of();

    /**
     * Prevents instantiation of this utility class.
     */
    private PluginManager() {
        // utility class
    }

    /**
     * Registers the single LOADER metadata.
     *
     * @param metadata loader metadata; must not be {@code null} and must have type {@link PluginType#LOADER}
     * @throws IllegalStateException    if already initialized or loader already registered
     * @throws IllegalArgumentException if {@code metadata} has an invalid type
     */
    public static void registerLoader(Metadata metadata) {
        Objects.requireNonNull(metadata, "metadata must not be null");
        if (metadata.getType() != PluginType.LOADER) {
            throw new IllegalArgumentException("Expected LOADER metadata, but got: " + metadata.getType());
        }

        synchronized (LOCK) {
            if (initialized) {
                throw new IllegalStateException("Cannot register loader after initialization");
            }
            if (registeredLoader != null) {
                throw new IllegalStateException("Loader is already registered: " + registeredLoader.getId());
            }
            registeredLoader = metadata;
            Logger.debug("Registered LOADER id='{}'", metadata.getId());
        }
    }

    /**
     * Registers the single PROVIDER metadata.
     *
     * @param metadata provider metadata; must not be {@code null} and must have type {@link PluginType#PROVIDER}
     * @throws IllegalStateException    if already initialized or provider already registered
     * @throws IllegalArgumentException if {@code metadata} has an invalid type
     */
    public static void registerProvider(Metadata metadata) {
        Objects.requireNonNull(metadata, "metadata must not be null");
        if (metadata.getType() != PluginType.PROVIDER) {
            throw new IllegalArgumentException("Expected PROVIDER metadata, but got: " + metadata.getType());
        }

        synchronized (LOCK) {
            if (initialized) {
                throw new IllegalStateException("Cannot register provider after initialization");
            }
            if (registeredProvider != null) {
                throw new IllegalStateException("Provider is already registered: " + registeredProvider.getId());
            }
            registeredProvider = metadata;
            Logger.debug("Registered PROVIDER id='{}'", metadata.getId());
        }
    }

    /**
     * Initializes plugins using the default plugins folder ({@link Constants#PLUGINS_FOLDER_NAME}).
     *
     * @param classLoader core classloader used to load plugin classes; must not be {@code null}
     */
    public static void initialize(AvrixClassLoader classLoader) {
        initialize(classLoader, Path.of(Constants.PLUGINS_FOLDER_NAME));
    }

    /**
     * Initializes plugins using the given plugins folder.
     *
     * @param classLoader   core classloader used to load plugin classes; must not be {@code null}
     * @param pluginsFolder folder that contains plugin JARs; must not be {@code null}
     * @throws IllegalStateException if initialization fails or was already executed
     */
    public static void initialize(AvrixClassLoader classLoader, Path pluginsFolder) {
        Objects.requireNonNull(classLoader, "classLoader must not be null");
        Objects.requireNonNull(pluginsFolder, "pluginsFolder must not be null");

        synchronized (LOCK) {
            if (initialized) {
                throw new IllegalStateException("Already initialized");
            }

            Metadata loader = registeredLoader;
            Metadata provider = registeredProvider;

            if (loader == null) {
                throw new IllegalStateException("LOADER is not registered");
            }
            if (provider == null) {
                throw new IllegalStateException("PROVIDER is not registered");
            }

            Logger.info("Initializing plugins from '{}'", pluginsFolder.toAbsolutePath());

            try {
                Files.createDirectories(pluginsFolder);
            } catch (IOException e) {
                throw new IllegalStateException("Failed to create plugins folder: " + pluginsFolder.toAbsolutePath(), e);
            }

            List<Path> jars = getJarFiles(pluginsFolder);
            Logger.info("Discovered {} plugin JAR(s)", jars.size());

            Map<String, Metadata> all = new HashMap<>(Math.max(16, jars.size() * 2));
            putUnique(all, loader, "registered LOADER");
            putUnique(all, provider, "registered PROVIDER");

            int skippedNoMetadata = 0;
            int skippedBadMetadata = 0;

            for (Path jar : jars) {
                Optional<Metadata> opt;
                try {
                    opt = Metadata.createFromJar(jar.toFile(), Constants.PLUGINS_METADATA_NAME);
                } catch (RuntimeException ex) {
                    skippedBadMetadata++;
                    Logger.debug(ex, "Failed to read metadata from '{}'", jar.getFileName());
                    continue;
                }

                if (opt.isEmpty()) {
                    skippedNoMetadata++;
                    Logger.debug("'{}' has no '{}' (or empty) -> skip",
                            jar.getFileName(), Constants.PLUGINS_METADATA_NAME);
                    continue;
                }

                Metadata md = opt.get();
                String id = requireNonBlank(md.getId(), "metadata.id");

                PluginType type = Objects.requireNonNull(md.getType(), "type must not be null");
                if (type == PluginType.LOADER || type == PluginType.PROVIDER) {
                    throw new IllegalArgumentException("LOADER/PROVIDER must be registered, not discovered from jar: id='"
                            + id + "' type=" + type + " jar=" + jar.getFileName());
                }

                Metadata prev = all.putIfAbsent(id, md);
                if (prev != null) {
                    throw new IllegalArgumentException("Duplicate plugin id '" + id + "' in '" + jar.getFileName()
                            + "' and '" + prev.getPluginFile() + "'");
                }

                attachJarToClassLoader(classLoader, jar);
                Logger.debug("Attached '{}' id='{}' type={}", jar.getFileName(), id, type);
            }

            Logger.info("Loaded metadata total={} (skipped: no-metadata={}, bad-metadata={})",
                    all.size(), skippedNoMetadata, skippedBadMetadata);

            validateDependenciesOrThrow(all);

            List<Metadata> sorted = LoadOrderSorter.sort(all.values());
            sortedMetadata = List.copyOf(sorted);

            Logger.info("Registered: loader='{}', provider='{}'", loader.getId(), provider.getId());
            Logger.debug("Load order: {}", sorted.stream().map(Metadata::getId).toList());

            List<Plugin> instances = new ArrayList<>();
            int entryPointCount = 0;

            for (Metadata md : sortedMetadata) {
                if (md.getType() != PluginType.PLUGIN) {
                    continue;
                }

                List<String> entryPoints = md.getEntryPoints();
                if (entryPoints == null || entryPoints.isEmpty()) {
                    Logger.debug("id='{}' has no entrypoints -> skip instantiation", md.getId());
                    continue;
                }

                for (String entryPoint : entryPoints) {
                    entryPointCount++;
                    Plugin plugin = instantiatePlugin(classLoader, md, entryPoint);
                    plugin.onInitialize();
                    instances.add(plugin);
                }
            }

            loadedPlugins = List.copyOf(instances);
            initialized = true;

            Logger.info("Initialized entrypoints={} (instances={})", entryPointCount, loadedPlugins.size());
        }
    }

    /**
     * Launches plugins that were previously initialized via {@link #initialize(AvrixClassLoader)}.
     *
     * @throws IllegalStateException if not initialized yet or already launched
     */
    public static void launch() {
        synchronized (LOCK) {
            if (!initialized) {
                throw new IllegalStateException("Not initialized");
            }
            if (launched) {
                throw new IllegalStateException("Already launched");
            }

            Logger.info("Launching {} plugin instance(s)", loadedPlugins.size());

            for (Plugin plugin : loadedPlugins) {
                plugin.onLaunch();
            }

            launched = true;
            Logger.info("PluginManager launch completed");
        }
    }

    /**
     * Returns instantiated plugin entrypoints (only {@link PluginType#PLUGIN}) after successful initialization.
     *
     * @return immutable list in initialization order
     */
    public static List<Plugin> getLoadedPlugins() {
        return loadedPlugins;
    }

    /**
     * Returns all {@code *.jar} files in the given folder (non-recursive), in deterministic order.
     */
    private static List<Path> getJarFiles(Path folder) {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(folder, "*.jar")) {
            List<Path> jars = new ArrayList<>();
            for (Path path : stream) {
                if (Files.isRegularFile(path)) {
                    jars.add(path);
                }
            }
            jars.sort(Comparator.comparing(path -> path.getFileName().toString(), String.CASE_INSENSITIVE_ORDER));
            return jars;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to list plugin jars in: " + folder.toAbsolutePath(), e);
        }
    }

    /**
     * Attaches a plugin JAR to the provided classloader.
     */
    private static void attachJarToClassLoader(AvrixClassLoader classLoader, Path jar) {
        try {
            classLoader.addURL(jar.toUri().toURL());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to attach jar to classloader: " + jar.toAbsolutePath(), e);
        }
    }

    /**
     * Instantiates a plugin entrypoint class using {@code (Metadata)} constructor.
     */
    private static Plugin instantiatePlugin(AvrixClassLoader classLoader, Metadata metadata, String entryPoint) {
        String point = requireNonBlank(entryPoint, "entryPoint");

        try {
            Class<?> raw = classLoader.loadClass(point);

            if (!Plugin.class.isAssignableFrom(raw)) {
                throw new IllegalStateException("Entrypoint '" + point + "' does not extend Plugin");
            }

            @SuppressWarnings("unchecked")
            Class<? extends Plugin> pluginClass = (Class<? extends Plugin>) raw;

            Constructor<? extends Plugin> constructor = pluginClass.getDeclaredConstructor(Metadata.class);
            constructor.setAccessible(true);

            return constructor.newInstance(metadata);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to instantiate plugin entrypoint '" + point + "'", e);
        }
    }

    /**
     * Adds metadata to the map by id ensuring uniqueness.
     */
    private static void putUnique(Map<String, Metadata> map, Metadata md, String origin) {
        String id = requireNonBlank(md.getId(), "metadata.id");
        Metadata prev = map.putIfAbsent(id, md);
        if (prev != null) {
            throw new IllegalArgumentException("Duplicate plugin id '" + id + "' (" + origin + ")");
        }
    }

    /**
     * Validates that a string value is non-null and not blank.
     */
    private static String requireNonBlank(String value, String field) {
        Objects.requireNonNull(value, field + " must not be null");
        if (value.trim().isEmpty()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }

    /**
     * Validates that all dependencies exist and their versions satisfy the declared constraints.
     *
     * <p>
     * The dependency map is interpreted as: {@code dependencyId -> versionCondition}.
     * A version condition of {@code "*"} or blank means "no version restriction".
     * </p>
     *
     * @param allById all discovered metadata including loader and provider
     * @throws IllegalArgumentException if a dependency is missing or a version constraint is not satisfied
     */
    private static void validateDependenciesOrThrow(Map<String, Metadata> allById) {
        long checked = 0;

        for (Metadata md : allById.values()) {
            Map<String, String> deps = md.getDependencies();
            if (deps == null || deps.isEmpty()) {
                continue;
            }

            for (Map.Entry<String, String> depEntry : deps.entrySet()) {
                String depId = requireNonBlank(depEntry.getKey(), "dependency.id");
                Metadata dep = allById.get(depId);
                if (dep == null) {
                    throw new IllegalArgumentException(
                            "Missing dependency '" + depId + "' required by '" + md.getId() + "'"
                    );
                }

                String condition = depEntry.getValue();
                if (condition == null || condition.isBlank() || condition.trim().equals("*")) {
                    checked++;
                    continue;
                }

                String depVersion = dep.getVersion();
                if (!VersionChecker.isVersionCompatible(depVersion, condition)) {
                    throw new IllegalArgumentException(
                            "Incompatible dependency version: '" + md.getId() + "' requires '" + depId + "' "
                                    + condition + " but found " + depVersion
                    );
                }

                checked++;
            }
        }

        Logger.info("Dependency check passed (checked={})", checked);
    }
}
