package com.avrix.plugins;

import com.avrix.core.Environment;
import com.avrix.core.KnotClassLoader;
import com.avrix.core.Metadata;
import com.avrix.core.ServiceManager;
import com.avrix.mixin.MixinTransformer;
import com.avrix.provider.GameProvider;
import com.avrix.provider.LoaderProvider;
import com.avrix.utils.Constants;
import com.github.zafarkhaja.semver.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * Production-ready default implementation of the {@link PluginManager} subsystem.
 * <p>
 * Orchestrates plugin discovery from disk, environment-specific filtering, dependency graph validation
 * with topological sorting via Kahn's algorithm, flat classpath registration in {@link KnotClassLoader},
 * mixin configuration loading, and entrypoint lifecycle invocation.
 */
public class DefaultPluginManager implements PluginManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultPluginManager.class);

    private final Map<String, PluginData> plugins = new ConcurrentHashMap<>();
    private final List<Metadata> corePlugins = new CopyOnWriteArrayList<>();
    private volatile Map<String, PluginData> cachedPlugins = Map.of();

    private KnotClassLoader classLoader;
    private volatile boolean initialized = false;

    /**
     * Initializes the plugin manager, creates the plugins storage directory if missing,
     * and registers core runtime metadata for the game provider and Avrix loader.
     *
     * @throws IllegalStateException if already initialized or if required services are missing
     */
    @Override
    public synchronized void init() {
        if (initialized) {
            throw new IllegalStateException("PluginManager is already initialized");
        }

        this.classLoader = ServiceManager.find(KnotClassLoader.class)
                .orElseThrow(() -> new IllegalStateException("KnotClassLoader is not registered in ServiceManager"));

        GameProvider gameProvider = ServiceManager.find(GameProvider.class)
                .orElseThrow(() -> new IllegalStateException("GameProvider is not registered in ServiceManager"));

        LoaderProvider loaderProvider = ServiceManager.find(LoaderProvider.class)
                .orElseThrow(() -> new IllegalStateException("LoaderProvider is not registered in ServiceManager"));

        // Register synthetic metadata descriptors for Game and Loader
        corePlugins.add(Metadata.fromGameProvider(gameProvider));
        corePlugins.add(loaderProvider.getMetadata());

        Path pluginsDir = Path.of(Constants.PLUGINS_FOLDER_NAME).toAbsolutePath();
        try {
            if (Files.notExists(pluginsDir)) {
                Files.createDirectories(pluginsDir);
                LOGGER.debug("Created plugins directory: [{}]", pluginsDir);
            } else if (!Files.isDirectory(pluginsDir)) {
                throw new IllegalStateException("Path '%s' exists but is not a directory. Remove it and restart.".formatted(pluginsDir));
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to create plugins directory: " + pluginsDir, e);
        }

        this.initialized = true;
        LOGGER.info("Plugin manager initialized successfully with {} core descriptor(s).", corePlugins.size());
    }

    /**
     * Discovers all plugin archives, filters them against the active environment,
     * resolves their topological execution order, and proceeds to load each plugin.
     *
     * @throws IllegalStateException if the manager is not initialized
     */
    @Override
    public void loadPlugins() {
        if (!initialized) {
            throw new IllegalStateException("PluginManager is not initialized. Call init() first.");
        }

        GameProvider provider = ServiceManager.get(GameProvider.class);
        Environment activeEnvironment = provider.getEnvironment();

        List<File> pluginFiles = getPluginFiles();
        if (pluginFiles.isEmpty()) {
            LOGGER.debug("No plugin archives discovered in [{}]", Constants.PLUGINS_FOLDER_NAME);
            return;
        }

        List<Metadata> candidates = new ArrayList<>(pluginFiles.size());
        Map<String, File> candidatesFiles = new HashMap<>();

        for (File pluginFile : pluginFiles) {
            try {
                Metadata metadata = Metadata.fromJarFile(pluginFile.toPath(), Constants.METADATA_NAME);

                if (!metadata.environment().isCompatibleWith(activeEnvironment)) {
                    LOGGER.debug("Skipping plugin [{}] due to environment mismatch (expected: {}, target: {})",
                            metadata.name(), activeEnvironment, metadata.environment());
                    continue;
                }

                candidates.add(metadata);
                candidatesFiles.put(metadata.id(), pluginFile);
                LOGGER.debug("Candidate plugin discovered: [{}] ({}@{})", metadata.name(), metadata.id(), metadata.version());
            } catch (Exception e) {
                LOGGER.warn("Failed to parse metadata from plugin archive [{}]. Skipping. Reason: {}",
                        pluginFile.getName(), e.getMessage());
            }
        }

        candidates.addAll(corePlugins);
        List<Metadata> sortedPlugins = resolvePluginLoadOrder(candidates);

        for (Metadata meta : sortedPlugins) {
            if (corePlugins.stream().anyMatch(core -> core.id().equals(meta.id()))) {
                continue;
            }

            File pluginFile = candidatesFiles.get(meta.id());
            PluginData container = new PluginData(pluginFile, buildIconUri(pluginFile), null, meta);

            try {
                loadPlugin(container);
            } catch (Exception e) {
                LOGGER.error("Failed to initialize plugin [{}]. Skipping execution.", meta.name(), e);
            }
        }

        LOGGER.info("Plugin manager active with {} user plugin(s) and {} core component(s).",
                plugins.size(), corePlugins.size());
    }

    /**
     * Loads an individual plugin into the flat {@link KnotClassLoader}, registers its mixins,
     * instantiates the entrypoint class, and executes {@link Plugin#onInitialize(PluginData)}.
     *
     * @param container plugin metadata and file container
     * @throws RuntimeException if attachment, mixin parsing, or entrypoint execution fails
     */
    @Override
    public void loadPlugin(PluginData container) {
        Objects.requireNonNull(container, "Plugin container cannot be null");
        Metadata meta = container.getMetadata();
        String id = meta.id();
        String entrypoint = meta.entrypoint();

        if (plugins.containsKey(id)) {
            LOGGER.debug("Plugin [{}] is already loaded. Skipping duplicate load request.", id);
            return;
        }

        File jarFile = container.getPluginFile()
                .orElseThrow(() -> new IllegalStateException("Cannot load user plugin '" + id + "' without a physical JAR file"));

        long startTime = System.nanoTime();
        Plugin instance = null;

        try {
            URL jarUrl = jarFile.toURI().toURL();

            // Attach plugin JAR to the flat KnotClassLoader
            classLoader.addURL(jarUrl);

            // Register mixin transformers
            if (!meta.mixins().isEmpty()) {
                MixinTransformer.addMixins(meta.mixins());
                LOGGER.debug("Registered {} mixin configuration(s) for plugin [{}]", meta.mixins().size(), id);
            }

            // Instantiate and initialize entrypoint if declared
            if (entrypoint != null && !entrypoint.isBlank()) {
                Class<?> pluginClass = Class.forName(entrypoint, true, classLoader);

                if (!Plugin.class.isAssignableFrom(pluginClass)) {
                    throw new IllegalArgumentException("Declared entrypoint [" + entrypoint + "] does not implement Plugin interface");
                }

                instance = (Plugin) pluginClass.getDeclaredConstructor().newInstance();
                instance.onInitialize(container);
                LOGGER.debug("Initialized entrypoint [{}] for plugin [{}]", entrypoint, id);
            }

            PluginData finalizedContainer = (instance != null)
                    ? new PluginData(jarFile, container.getPluginIconURI().orElse(null), instance, meta)
                    : container;

            plugins.put(id, finalizedContainer);
            cachedPlugins = Map.of();

            long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
            String pluginType = (entrypoint != null && !entrypoint.isBlank()) ? "plugin" : "library";
            LOGGER.info("Loaded {} [{}] ({}@{}) in {}ms", pluginType, meta.name(), id, meta.version(), elapsedMillis);

        } catch (Exception e) {
            LOGGER.error("Failed to load plugin [{}]: {}", id, e.getMessage(), e);
            throw new RuntimeException("Failed to load plugin: " + id, e);
        }
    }

    /**
     * Returns an unmodifiable map containing all loaded user and core plugins.
     *
     * @return unmodifiable map of plugin IDs to their active containers
     */
    @Override
    public Map<String, PluginData> getPlugins() {
        Map<String, PluginData> cache = cachedPlugins;
        if (cache.isEmpty()) {
            Map<String, PluginData> merged = new ConcurrentHashMap<>(plugins);
            for (Metadata coreMeta : corePlugins) {
                merged.putIfAbsent(coreMeta.id(), new PluginData(coreMeta));
            }
            cache = Collections.unmodifiableMap(merged);
            cachedPlugins = cache;
        }
        return cache;
    }

    /**
     * Computes the execution order of candidate plugins using topological sorting
     * and performs strict SemVer constraint satisfaction verification.
     *
     * @param candidates list of candidate metadata descriptors
     * @return ordered list of user plugin metadata ready for sequential initialization
     * @throws IllegalStateException if missing dependencies, constraint violations, or cycles are detected
     */
    @Override
    public List<Metadata> resolvePluginLoadOrder(List<Metadata> candidates) {
        Objects.requireNonNull(candidates, "Candidates list cannot be null");

        Map<String, Metadata> registry = new HashMap<>();
        for (Metadata meta : candidates) {
            if (registry.putIfAbsent(meta.id(), meta) != null) {
                LOGGER.warn("Duplicate plugin identifier [{}]. The first detected instance will be retained.", meta.id());
            }
        }

        Map<String, Integer> inDegrees = new HashMap<>();
        Map<String, List<String>> adjacencyList = new HashMap<>();

        for (String id : registry.keySet()) {
            inDegrees.put(id, 0);
            adjacencyList.put(id, new ArrayList<>());
        }

        for (Metadata meta : registry.values()) {
            String dependentId = meta.id();

            for (Map.Entry<String, String> dependency : meta.dependencies().entrySet()) {
                String requiredId = dependency.getKey();
                String constraint = dependency.getValue();
                Metadata requiredMeta = registry.get(requiredId);

                if (requiredMeta == null) {
                    throw new IllegalStateException(
                            "Plugin [%s] depends on missing component [%s]".formatted(dependentId, requiredId)
                    );
                }

                try {
                    Version actualVersion = Version.parse(requiredMeta.version());
                    if (!actualVersion.satisfies(constraint)) {
                        throw new IllegalStateException(
                                "Plugin [%s] requires [%s] matching [%s], but found version [%s]"
                                        .formatted(dependentId, requiredId, constraint, requiredMeta.version())
                        );
                    }
                } catch (Exception e) {
                    throw new IllegalStateException(
                            "Invalid version constraint in plugin [%s] for dependency [%s]: %s"
                                    .formatted(dependentId, requiredId, e.getMessage()), e
                    );
                }

                adjacencyList.get(requiredId).add(dependentId);
                inDegrees.put(dependentId, inDegrees.get(dependentId) + 1);
            }
        }

        Queue<String> queue = new LinkedList<>();
        for (Map.Entry<String, Integer> entry : inDegrees.entrySet()) {
            if (entry.getValue() == 0) {
                queue.add(entry.getKey());
            }
        }

        List<Metadata> sortedList = new ArrayList<>(registry.size());
        while (!queue.isEmpty()) {
            String currentId = queue.poll();
            sortedList.add(registry.get(currentId));

            for (String neighbor : adjacencyList.get(currentId)) {
                int updatedDegree = inDegrees.get(neighbor) - 1;
                inDegrees.put(neighbor, updatedDegree);

                if (updatedDegree == 0) {
                    queue.add(neighbor);
                }
            }
        }

        if (sortedList.size() != registry.size()) {
            List<String> cycleNodes = inDegrees.entrySet().stream()
                    .filter(entry -> entry.getValue() > 0)
                    .map(Map.Entry::getKey)
                    .toList();
            throw new IllegalStateException("Cyclic plugin dependency detected. Circular nodes: " + String.join(", ", cycleNodes));
        }

        List<Metadata> userPluginsOrder = sortedList.stream()
                .filter(meta -> corePlugins.stream().noneMatch(core -> core.id().equals(meta.id())))
                .toList();

        LOGGER.debug("Topological sort resolved: {} user plugin(s) ({} core entries omitted from execution queue)",
                userPluginsOrder.size(), corePlugins.size());

        return List.copyOf(userPluginsOrder);
    }

    /**
     * Constructs the URI pointing to an embedded icon resource within a plugin JAR.
     *
     * @param file the plugin physical file
     * @return icon URI, or {@code null} if file is null or URI creation fails
     */
    private URI buildIconUri(File file) {
        if (file == null) {
            return null;
        }
        try {
            return new URI("jar:" + file.toURI() + "!/" + Constants.PLUGIN_ICON_NAME);
        } catch (URISyntaxException _) {
            return null;
        }
    }

    /**
     * Scans the plugins folder on disk and returns valid plugin file references.
     *
     * @return list of JAR files matching the plugin extension
     */
    private List<File> getPluginFiles() {
        Path pluginsDir = Path.of(Constants.PLUGINS_FOLDER_NAME);
        if (!Files.isDirectory(pluginsDir)) {
            return List.of();
        }

        try (Stream<Path> stream = Files.list(pluginsDir)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(Constants.PLUGIN_EXTENSION))
                    .map(Path::toFile)
                    .toList();
        } catch (IOException e) {
            LOGGER.error("Failed to enumerate plugin archives in [{}]", pluginsDir, e);
            return List.of();
        }
    }
}