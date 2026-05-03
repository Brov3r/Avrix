package com.avrix.plugins;

import com.avrix.core.AvrixBootstrap;
import com.avrix.core.BaseClassLoader;
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
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * Manages the lifecycle of Avrix plugins: discovery, loading, initialization and launching.
 * Maintains separate registries for core framework plugins and user-installed plugins.
 */
public class AvrixPluginManager implements PluginManager {
    private static final Logger log = LoggerFactory.getLogger(AvrixPluginManager.class);
    private final Map<String, PluginContainer> PLUGINS = new ConcurrentHashMap<>();
    private final List<Metadata> CORE_PLUGINS = new CopyOnWriteArrayList<>();

    private volatile Map<String, PluginContainer> cachedPlugins = Map.of();

    private static BaseClassLoader classLoader;

    private static volatile boolean initialized = false;

    /**
     * Initializes the plugin manager: registers core plugins and prepares the plugins directory.
     *
     * @throws IllegalStateException if called more than once, or if plugins path is not a directory
     * @throws UncheckedIOException  if directory creation fails
     */
    @Override
    public void init(BaseClassLoader loader) {
        if (initialized) {
            throw new IllegalStateException("PluginManager is already initialized");
        }

        classLoader = Objects.requireNonNull(loader, "ClassLoader must not be null");

        CORE_PLUGINS.add(Metadata.fromGameProvider(AvrixBootstrap.getInstance().getGameProvider()));
        CORE_PLUGINS.add(Metadata.fromBootstrap(AvrixBootstrap.getInstance()));

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
     * @return Immutable {@link Map} of loaded plugins keyed by plugin ID, value - {@link PluginContainer}.
     * Never {@code null}; may be empty if no plugins loaded.
     */
    @Override
    public Map<String, PluginContainer> getPlugins() {
        if (cachedPlugins.isEmpty()) {
            Map<String, PluginContainer> merged = new ConcurrentHashMap<>(PLUGINS);
            CORE_PLUGINS.forEach(meta -> merged.putIfAbsent(meta.getId(), new PluginContainer(meta)));
            cachedPlugins = Collections.unmodifiableMap(merged);
        }

        return cachedPlugins;
    }

    /**
     * Reset plugin cache, used when adding/removing plugins
     */
    private void invalidateCache() {
        if (cachedPlugins.isEmpty()) return;
        cachedPlugins = Map.of();
    }

    /**
     * Discovers, validates, sorts, and loads all compatible plugins.
     * <p>
     * Faulty plugins are isolated and skipped without aborting the loading process.
     */
    @Override
    public void loadPlugins() {
        if (!initialized) {
            throw new IllegalStateException("PluginManager is not initialized. Call init() first.");
        }

        GameProvider provider = AvrixBootstrap.getInstance().getGameProvider();
        Environment targetEnv = provider.getEnvironment();

        List<File> pluginPaths = getPluginFiles();
        List<Metadata> candidates = new ArrayList<>(pluginPaths.size());
        Map<String, File> candidatesFiles = new HashMap<>();

        if (pluginPaths.isEmpty()) return;
        
        for (File pluginFile : pluginPaths) {
            try {
                Metadata meta = Metadata.fromJarFile(pluginFile, Constants.METADATA_NAME);

                if (meta.getEnvironment() != targetEnv && meta.getEnvironment() != Environment.BOTH) {
                    log.debug("Skipping '{}': incompatible environment (expected: {}, found: {})",
                            meta.getName(), targetEnv, meta.getEnvironment());
                    continue;
                }
                candidates.add(meta);
                candidatesFiles.put(meta.getId(), pluginFile);
                log.debug("Candidate plugin detected: {} ({}@{})", meta.getName(), meta.getId(), meta.getVersion());
            } catch (Exception e) {
                log.warn("Failed to read metadata from '{}'. Skipping. Reason: {}", pluginFile.getName(), e.getMessage());
            }
        }

        candidates.addAll(CORE_PLUGINS);

        List<Metadata> sortedPlugins = resolvePluginLoadOrder(candidates);

        for (Metadata meta : sortedPlugins) {
            File pluginFile = candidatesFiles.get(meta.getId());

            PluginContainer container = new PluginContainer(pluginFile, buildIconUri(pluginFile), null, meta);

            try {
                classLoader.addURL(pluginFile.toURI().toURL());
                loadPlugin(container);
            } catch (Exception e) {
                log.error("Failed to initialize plugin '{}'. Skipping.", meta.getName(), e);
            }
        }

        log.info("Plugin manager has launched with {} user plugins and {} core plugins...", PLUGINS.size(), CORE_PLUGINS.size());
    }

    /**
     * Building the URI of the plugin icon
     *
     * @param file JAR file plugin
     * @return a ready-made plugin icon URI, or null if there is no file or the icon does not exist.
     */
    private URI buildIconUri(File file) {
        if (file == null) return null;
        try {
            return new URI("jar:" + file.toURI() + "!/" + Constants.PLUGINS_ICON_NAME);
        } catch (URISyntaxException e) {
            return null;
        }
    }

    /**
     * Loads and initializes a single plugin from its {@link PluginContainer}.
     *
     * @param container Validated {@link PluginContainer} with {@link Metadata} and file references.
     */
    @Override
    public void loadPlugin(PluginContainer container) {
        Metadata meta = container.getMetadata();
        String id = meta.getId();
        String entrypoint = meta.getEntrypoint();

        // Skip if already loaded
        if (PLUGINS.containsKey(id)) {
            log.debug("Plugin {} already loaded, skipping", id);
            return;
        }

        long start = System.nanoTime();
        Plugin instance = null;

        try {
            if (meta.getMixins() != null && !meta.getMixins().isEmpty()) {
                MixinAgent.addMixins(meta.getMixins());
                log.debug("Registered {} mixin configs for plugin {}", meta.getMixins().size(), id);
            }

            if (entrypoint != null && !entrypoint.isBlank()) {
                Class<?> pluginClass = Class.forName(entrypoint, true, classLoader);

                if (!Plugin.class.isAssignableFrom(pluginClass)) {
                    throw new RuntimeException("Entrypoint '" + entrypoint + "' does not implement Plugin");
                }

                instance = (Plugin) pluginClass
                        .getDeclaredConstructor(Metadata.class, File.class, URI.class)
                        .newInstance(meta, container.getPluginFile(), container.getPluginIconURI());

                instance.onInitialize();
                log.debug("Initialized entrypoint class '{}' for plugin {}", entrypoint, id);
            }

            PLUGINS.put(id, instance != null
                    ? new PluginContainer(container.getPluginFile(), container.getPluginIconURI(), instance, meta)
                    : container);

            invalidateCache();

            long duration = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
            String type = (entrypoint != null && !entrypoint.isBlank()) ? "plugin" : "library";
            log.info("Loaded {} {} ({}@{}) in {}ms", type, meta.getName(), id, meta.getVersion(), duration);

        } catch (ClassNotFoundException e) {
            log.error("Failed to load plugin '{}': entrypoint class '{}' not found", id, entrypoint, e);
            throw new RuntimeException("Entrypoint not found: " + entrypoint, e);
        } catch (NoSuchMethodException e) {
            log.error("Plugin '{}' missing required constructor (Metadata, File, Path)", id, e);
            throw new RuntimeException("Invalid plugin constructor", e);
        } catch (IllegalAccessException | InvocationTargetException | InstantiationException e) {
            log.error("Failed to instantiate plugin '{}': {}", id, e.getMessage(), e);
            throw new RuntimeException("Instantiation failed", e);
        } catch (ClassCastException e) {
            log.error("Plugin '{}' entrypoint does not implement Plugin abstract class", id, e);
            throw new RuntimeException("Invalid plugin type", e);
        } catch (Throwable t) {
            log.error("Unexpected error loading plugin '{}': {}", id, t.getMessage(), t);
            throw new RuntimeException("Unexpected error", t);
        }
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
    @Override
    public List<Metadata> resolvePluginLoadOrder(List<Metadata> candidates) {
        Map<String, Metadata> registry = new HashMap<>();
        for (Metadata meta : candidates) {
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
     */
    private List<File> getPluginFiles() {
        Path pluginsDir = Path.of(Constants.PLUGINS_FOLDER_NAME);
        if (!Files.isDirectory(pluginsDir)) {
            return List.of();
        }
        List<File> fileList = new ArrayList<>();

        try (Stream<Path> stream = Files.list(pluginsDir)) {
            fileList = stream
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(Constants.PLUGIN_EXTENSION))
                    .map(Path::toFile)
                    .toList();
        } catch (Exception e) {
            log.error("Couldn't get a list of potential plugins!", e);
        }

        log.debug("Found {} potential plugin file...", fileList.size());

        return fileList;
    }
}
