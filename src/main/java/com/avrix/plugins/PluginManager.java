package com.avrix.plugins;

import com.avrix.core.BaseClassLoader;
import com.avrix.core.Bootstrap;
import com.avrix.core.Environment;
import com.avrix.core.Metadata;
import com.avrix.mixin.MixinAgent;
import com.avrix.provider.GameProvider;
import com.avrix.utils.Constants;
import com.github.zafarkhaja.semver.Version;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.traverse.TopologicalOrderIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;

/**
 * Manages the lifecycle of Avrix plugins: discovery, loading, initialization and launching.
 * Maintains separate registries for core framework plugins and user-installed plugins.
 */
public class PluginManager {

    /**
     * Logger instance for this class.
     */
    private static final Logger log = LoggerFactory.getLogger(PluginManager.class);

    /**
     * List of loaded user plugins; thread-safe for concurrent reads/writes.
     */
    private static final List<Metadata> LOADED_PLUGINS = new CopyOnWriteArrayList<>();

    /**
     * List of core system plugins (loader, game provider); thread-safe.
     */
    private static final List<Metadata> CORE_PLUGINS = new CopyOnWriteArrayList<>();

    /**
     * Flag indicating whether {@link #init()} has completed successfully.
     */
    private static volatile boolean initialized = false;

    /**
     * Initializes the plugin manager: registers core plugins and prepares the plugins directory.
     *
     * @throws IllegalStateException if called more than once, or if plugins path is not a directory
     * @throws UncheckedIOException  if directory creation fails
     */
    public static void init() {
        if (initialized) {
            throw new IllegalStateException("PluginManager is already initialized");
        }

        CORE_PLUGINS.add(Metadata.fromGameProvider(Bootstrap.getGameProvider()));
        CORE_PLUGINS.add(Metadata.getLoaderMetadata());

        Path pluginsDir = Path.of(Constants.PLUGINS_FOLDER_NAME).toAbsolutePath();
        try {
            if (Files.notExists(pluginsDir)) {
                Files.createDirectories(pluginsDir);
                log.debug("Created plugins directory: {}", pluginsDir);
            } else if (!Files.isDirectory(pluginsDir)) {
                throw new IllegalStateException("Path '%s' exists but is not a directory. Remove it and restart.".formatted(pluginsDir));
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to setup plugins directory: " + pluginsDir, e);
        }

        initialized = true;
        log.info("Plugin manager initialized successfully with {} core entries.", CORE_PLUGINS.size());
    }

    /**
     * Discovers, validates, sorts, and loads all compatible plugins.
     * <p>
     * Faulty plugins are isolated and skipped without aborting the loading process.
     *
     * @throws IOException if the plugins directory cannot be accessed
     */
    public static void loadPlugins() throws IOException {
        if (!initialized) {
            throw new IllegalStateException("PluginManager is not initialized. Call init() first.");
        }

        GameProvider provider = Bootstrap.getGameProvider();
        BaseClassLoader classLoader = Bootstrap.getClassLoader();
        Environment targetEnv = provider.getEnvironment();

        List<File> pluginPaths = getPluginFiles();
        List<Metadata> candidates = new ArrayList<>(pluginPaths.size());

        for (File pluginFile : pluginPaths) {
            try {
                Metadata meta = Metadata.fromJarFile(pluginFile, Constants.METADATA_NAME);

                if (meta.getEnvironment() != targetEnv && meta.getEnvironment() != Environment.BOTH) {
                    log.debug("Skipping '{}': incompatible environment (expected: {}, found: {})",
                            meta.getName(), targetEnv, meta.getEnvironment());
                    continue;
                }
                candidates.add(meta);
                log.debug("Candidate plugin detected: {} ({}@{})", meta.getName(), meta.getId(), meta.getVersion());
            } catch (Exception e) {
                log.warn("Failed to read metadata from '{}'. Skipping. Reason: {}", pluginFile.getName(), e.getMessage());
            }
        }

        List<Metadata> sortedPlugins = resolvePluginLoadOrder(candidates);

        for (Metadata meta : sortedPlugins) {
            try {
                classLoader.addURL(meta.getPluginFile().toURI().toURL());
                loadPlugin(meta);

                LOADED_PLUGINS.add(meta);

                log.info("Loaded plugin {} ({}@{})", meta.getName(), meta.getId(), meta.getVersion());
            } catch (Exception e) {
                log.error("Failed to initialize plugin '{}'. Skipping.", meta.getName(), e);
            }
        }

        log.info("Plugin manager has launched with {} user plugins and {} core plugins...", LOADED_PLUGINS.size(), CORE_PLUGINS.size());
    }

    /**
     * Loads and initializes a single plugin from its {@link Metadata}.
     * Registers Mixin configurations, instantiates the entrypoint class via reflection,
     * and invokes its {@link Plugin#onInitialize()} hook.
     *
     * @param meta plugin {@link Metadata} containing entrypoint class name and Mixin configs
     * @throws ClassNotFoundException    if the entrypoint class cannot be found
     * @throws NoSuchMethodException     if the constructor {@code Plugin(Metadata)} is missing
     * @throws InstantiationException    if the plugin class cannot be instantiated
     * @throws IllegalAccessException    if the constructor is not accessible
     * @throws InvocationTargetException if the plugin constructor or {@code onInitialize()} throws an exception
     */
    private static void loadPlugin(Metadata meta) throws ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
        if (meta.getEntrypoint() == null || meta.getEntrypoint().isBlank()) {
            return;
        }

        MixinAgent.addMixins(meta.getMixins());

        Class<?> pluginClass = Class.forName(meta.getEntrypoint(), true, Bootstrap.getClassLoader());
        Plugin pluginInstance = (Plugin) pluginClass.getDeclaredConstructor(Metadata.class).newInstance(meta);
        pluginInstance.onInitialize();
    }

    /**
     * Resolves a deterministic, dependency-aware plugin loading order.
     * <p>
     * Validates dependency presence, checks version constraints via SemVer,
     * removes transitively invalid plugins, detects cycles, and returns
     * a topologically sorted list.
     *
     * @param candidates discovered plugin {@link Metadata}
     * @return immutable list sorted by dependency graph
     * @throws IllegalStateException if circular dependencies are detected
     */
    private static List<Metadata> resolvePluginLoadOrder(List<Metadata> candidates) {
        List<Metadata> allPlugins = new ArrayList<>(candidates);
        allPlugins.addAll(CORE_PLUGINS);

        Map<String, Metadata> registry = new HashMap<>();
        for (Metadata meta : allPlugins) {
            if (registry.putIfAbsent(meta.getId(), meta) != null) {
                log.warn("Duplicate plugin ID '{}'. First occurrence will be used.", meta.getId());
            }
        }

        for (Metadata meta : registry.values()) {
            for (Map.Entry<String, String> dep : meta.getDependencies().entrySet()) {
                String depId = dep.getKey();
                String constraint = dep.getValue();
                Metadata depMeta = registry.get(depId);

                if (depMeta == null) {
                    throw new IllegalStateException(
                            "Plugin '%s' requires missing dependency '%s'".formatted(meta.getId(), depId));
                }

                try {
                    Version actual = Version.parse(depMeta.getVersion());
                    if (!actual.satisfies(constraint)) {
                        throw new IllegalStateException(
                                "Plugin '%s' requires '%s' matching '%s', but found '%s'"
                                        .formatted(meta.getId(), depId, constraint, depMeta.getVersion()));
                    }
                } catch (Exception e) {
                    throw new IllegalStateException(
                            "Invalid version/constraint in plugin '%s' dependency '%s': %s"
                                    .formatted(meta.getId(), depId, e.getMessage()), e);
                }
            }
        }

        Graph<String, DefaultEdge> graph = new DefaultDirectedGraph<>(DefaultEdge.class);
        registry.keySet().forEach(graph::addVertex);
        for (Metadata meta : registry.values()) {
            for (String depId : meta.getDependencies().keySet()) {
                graph.addEdge(depId, meta.getId());
            }
        }

        try {
            TopologicalOrderIterator<String, DefaultEdge> iterator = new TopologicalOrderIterator<>(graph);
            List<Metadata> sorted = new ArrayList<>(graph.vertexSet().size());
            while (iterator.hasNext()) {
                sorted.add(registry.get(iterator.next()));
            }

            List<Metadata> preparedList = sorted.stream()
                    .filter(m -> CORE_PLUGINS.stream().noneMatch(core -> core.getId().equals(m.getId())))
                    .toList();

            log.debug("Resolved load order: {} user plugins ({} core excluded)",
                    preparedList.size(), CORE_PLUGINS.size());

            return List.copyOf(preparedList);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Circular plugin dependency detected. Cannot resolve load order.", e);
        }
    }

    /**
     * Scans the plugins directory for valid plugin files.
     *
     * @return an immutable list of discovered plugin paths
     * @throws IOException if an I/O error occurs during directory traversal
     */
    private static List<File> getPluginFiles() throws IOException {
        Path pluginsDir = Path.of(Constants.PLUGINS_FOLDER_NAME);
        if (!Files.isDirectory(pluginsDir)) {
            return List.of();
        }
        List<File> fileList;

        try (Stream<Path> stream = Files.list(pluginsDir)) {
            fileList = stream
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(Constants.PLUGIN_EXTENSION))
                    .map(Path::toFile)
                    .toList();
        }

        log.debug("Found {} potential plugin file...", fileList.size());

        return fileList;
    }
}
