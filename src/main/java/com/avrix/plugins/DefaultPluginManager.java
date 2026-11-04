package com.avrix.plugins;

import com.avrix.core.Environment;
import com.avrix.core.KnotClassLoader;
import com.avrix.core.LoaderConfig;
import com.avrix.core.Metadata;
import com.avrix.core.MixinRegistrar;
import com.avrix.provider.GameProvider;
import com.avrix.provider.LoaderProvider;
import com.avrix.utils.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

/**
 * Production-ready default implementation of the {@link PluginManager} subsystem.
 * <p>
 * Orchestrates plugin discovery from disk, environment-specific filtering, dependency graph validation
 * with topological sorting rules, wildcard {@code "*"} ordering, flat classpath registration in
 * {@link KnotClassLoader}, mixin configuration loading, and entrypoint lifecycle invocation.
 */
public class DefaultPluginManager implements PluginManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultPluginManager.class);

    private final LoaderConfig config;
    private final KnotClassLoader classLoader;
    private final GameProvider gameProvider;
    private final LoaderProvider loaderProvider;
    private final MixinRegistrar mixinRegistrar;
    private final PluginScanner pluginScanner;
    private final PluginManifestParser manifestParser;
    private final Map<String, PluginData> plugins = new ConcurrentHashMap<>();
    private final List<Metadata> corePlugins = new CopyOnWriteArrayList<>();
    private final List<PluginLoadResult> loadResults = new CopyOnWriteArrayList<>();
    private final Set<String> preparedPlugins = ConcurrentHashMap.newKeySet();
    private final List<String> loadedPluginOrder = new CopyOnWriteArrayList<>();
    private volatile Map<String, PluginData> cachedPlugins = Map.of();

    private volatile boolean initialized = false;
    private volatile boolean closed = false;

    /**
     * Constructs a new plugin manager with explicit runtime dependencies.
     *
     * @param config         loader configuration
     * @param classLoader    flat classloader used to attach plugins and resolve classes
     * @param gameProvider   active game provider
     * @param loaderProvider loader metadata provider
     * @param mixinRegistrar registrar used for plugin mixin classes
     * @param pluginScanner  scanner used to discover plugin archives
     * @param manifestParser parser used to read plugin manifests
     * @throws NullPointerException if any dependency is null
     */
    public DefaultPluginManager(
            LoaderConfig config,
            KnotClassLoader classLoader,
            GameProvider gameProvider,
            LoaderProvider loaderProvider,
            MixinRegistrar mixinRegistrar,
            PluginScanner pluginScanner,
            PluginManifestParser manifestParser
    ) {
        this.config = Objects.requireNonNull(config, "Loader config cannot be null");
        this.classLoader = Objects.requireNonNull(classLoader, "KnotClassLoader cannot be null");
        this.gameProvider = Objects.requireNonNull(gameProvider, "GameProvider cannot be null");
        this.loaderProvider = Objects.requireNonNull(loaderProvider, "LoaderProvider cannot be null");
        this.mixinRegistrar = Objects.requireNonNull(mixinRegistrar, "Mixin registrar cannot be null");
        this.pluginScanner = Objects.requireNonNull(pluginScanner, "Plugin scanner cannot be null");
        this.manifestParser = Objects.requireNonNull(manifestParser, "Plugin manifest parser cannot be null");
    }

    /**
     * Initializes the plugin manager, creates the plugins storage directory if missing,
     * and registers core runtime metadata for the game provider and Avrix loader.
     *
     * @throws IllegalStateException if already initialized
     */
    @Override
    public synchronized void init() {
        if (closed) {
            throw new IllegalStateException("PluginManager is closed");
        }
        if (initialized) {
            throw new IllegalStateException("PluginManager is already initialized");
        }

        // Register synthetic metadata descriptors for Game and Loader
        corePlugins.add(gameProvider.getMetadata());
        corePlugins.add(loaderProvider.getMetadata());

        Path pluginsDir = config.pluginsDirectory().toAbsolutePath().normalize();
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
        if (closed) {
            throw new IllegalStateException("PluginManager is closed");
        }
        if (!initialized) {
            throw new IllegalStateException("PluginManager is not initialized. Call init() first.");
        }

        loadResults.clear();
        Environment activeEnvironment = config.environment();
        Set<String> disabledPlugins = pluginScanner.readDisabledPlugins();

        List<File> pluginFiles = pluginScanner.scan();
        if (pluginFiles.isEmpty()) {
            LOGGER.debug("No plugin archives discovered in [{}]", config.pluginsDirectory());
            return;
        }

        List<Metadata> candidates = new ArrayList<>(pluginFiles.size());
        Map<String, File> candidatesFiles = new HashMap<>();

        for (File pluginFile : pluginFiles) {
            String pluginFileName = pluginFile.getName();
            if (disabledPlugins.contains(pluginFileName.toLowerCase(Locale.ROOT))) {
                LOGGER.debug("Skipping plugin archive [{}] because it is listed in {}",
                        pluginFileName, Constants.DISABLED_PLUGINS_FILE_NAME);
                loadResults.add(PluginLoadResult.skipped(pluginFileName, "Disabled by " + Constants.DISABLED_PLUGINS_FILE_NAME));
                continue;
            }

            try {
                Metadata metadata = manifestParser.fromJarFile(pluginFile.toPath(), config.metadataFileName());

                if (isPluginDisabled(disabledPlugins, metadata, pluginFile)) {
                    LOGGER.debug("Skipping plugin [{}] because it is listed in {}", metadata.name(), Constants.DISABLED_PLUGINS_FILE_NAME);
                    loadResults.add(PluginLoadResult.skipped(metadata.id(), "Disabled by " + Constants.DISABLED_PLUGINS_FILE_NAME));
                    continue;
                }

                if (!metadata.environment().isCompatibleWith(activeEnvironment)) {
                    LOGGER.debug("Skipping plugin [{}] due to environment mismatch (expected: {}, target: {})",
                            metadata.name(), activeEnvironment, metadata.environment());
                    loadResults.add(PluginLoadResult.skipped(metadata.id(), "Environment mismatch"));
                    continue;
                }

                candidates.add(metadata);
                candidatesFiles.put(metadata.id(), pluginFile);
                LOGGER.debug("Candidate plugin discovered: [{}] ({}@{})", metadata.name(), metadata.id(), metadata.version());
            } catch (Throwable throwable) {
                LOGGER.warn("Failed to parse metadata from plugin archive [{}]. Skipping. Reason: {}",
                        pluginFile.getName(), throwable.getMessage());
                loadResults.add(PluginLoadResult.failed(pluginFile.getName(), throwable));

                if (config.strict()) {
                    throw new IllegalStateException("Failed to parse plugin metadata: " + pluginFile.getName(), throwable);
                }
            }
        }

        candidates.addAll(corePlugins);

        List<Metadata> sortedPlugins;
        try {
            sortedPlugins = resolvePluginLoadOrder(candidates);
        } catch (Throwable throwable) {
            LOGGER.error("Failed to resolve plugin load order", throwable);
            for (Metadata candidate : candidates) {
                if (corePlugins.stream().noneMatch(core -> core.id().equals(candidate.id()))) {
                    loadResults.add(PluginLoadResult.failed(candidate.id(), throwable));
                }
            }

            if (config.strict()) {
                throw new IllegalStateException("Failed to resolve plugin load order", throwable);
            }
            logLoadSummary();
            return;
        }

        List<PluginData> preparedPlugins = new ArrayList<>(sortedPlugins.size());
        for (Metadata meta : sortedPlugins) {
            if (corePlugins.stream().anyMatch(core -> core.id().equals(meta.id()))) {
                continue;
            }

            File pluginFile = candidatesFiles.get(meta.id());
            PluginData container = new PluginData(pluginFile, buildIconUri(pluginFile), null, meta);

            try {
                if (preparePlugin(container)) {
                    preparedPlugins.add(container);
                }
            } catch (Throwable throwable) {
                LOGGER.error("Failed to prepare plugin [{}]. Skipping execution.", meta.name(), throwable);
                loadResults.add(PluginLoadResult.failed(meta.id(), throwable));

                if (config.strict()) {
                    throw new IllegalStateException("Plugin preparation failed: " + meta.id(), throwable);
                }
            }
        }

        for (PluginData container : preparedPlugins) {
            String pluginId = container.metadata().id();
            try {
                startPlugin(container);
                loadResults.add(PluginLoadResult.loaded(pluginId));
            } catch (Throwable throwable) {
                LOGGER.error("Failed to initialize plugin [{}]. Skipping execution.", container.metadata().name(), throwable);
                loadResults.add(PluginLoadResult.failed(pluginId, throwable));

                if (config.strict()) {
                    throw new IllegalStateException("Plugin load failed: " + pluginId, throwable);
                }
            }
        }

        logLoadSummary();
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
        if (closed) {
            throw new IllegalStateException("PluginManager is closed");
        }
        if (!initialized) {
            throw new IllegalStateException("PluginManager is not initialized. Call init() first.");
        }
        Objects.requireNonNull(container, "Plugin container cannot be null");

        Metadata meta = container.metadata();
        String id = meta.id();

        if (plugins.containsKey(id)) {
            LOGGER.debug("Plugin [{}] is already loaded. Skipping duplicate load request.", id);
            return;
        }

        container.getPluginFile()
                .orElseThrow(() -> new IllegalStateException(
                        "Cannot load user plugin '" + id + "' without a physical JAR file"
                ));

        try {
            preparePlugin(container);
            startPlugin(container);
        } catch (Throwable throwable) {
            LOGGER.error("Failed to load plugin [{}]: {}", id, throwable.getMessage(), throwable);
            throw new RuntimeException("Failed to load plugin: " + id, throwable);
        }
    }

    /**
     * Attaches a plugin archive and registers its mixins without starting the entrypoint.
     *
     * @param container plugin metadata and file container
     * @return {@code true} if the plugin is ready for startup, {@code false} if it is already loaded
     * @throws IllegalStateException if the plugin has no physical JAR file
     * @throws MalformedURLException if the plugin archive cannot be converted to a URL
     */
    private boolean preparePlugin(PluginData container) throws MalformedURLException {
        Metadata meta = container.metadata();
        String id = meta.id();

        if (plugins.containsKey(id)) {
            return false;
        }
        if (preparedPlugins.contains(id)) {
            return true;
        }

        File jarFile = container.getPluginFile()
                .orElseThrow(() -> new IllegalStateException("Cannot load user plugin '" + id + "' without a physical JAR file"));

        URL jarUrl = jarFile.toURI().toURL();
        classLoader.addURL(jarUrl);

        if (!meta.mixins().isEmpty()) {
            for (String mixin : meta.mixins()) {
                mixinRegistrar.addMixin(mixin);
            }
            LOGGER.debug("Registered {} mixin configuration(s) for plugin [{}]", meta.mixins().size(), id);
        }

        preparedPlugins.add(id);
        LOGGER.debug("Prepared plugin [{}] ({})", meta.name(), id);
        return true;
    }

    /**
     * Starts a prepared plugin by creating its entrypoint and invoking {@link Plugin#onInitialize(PluginData)}.
     *
     * @param container plugin metadata and file container
     * @throws IllegalStateException  if the plugin was not prepared or cannot be started
     * @throws ReflectiveOperationException if the entrypoint cannot be loaded or instantiated
     */
    private void startPlugin(PluginData container) throws ReflectiveOperationException {
        Metadata meta = container.metadata();
        String id = meta.id();
        String entrypoint = meta.entrypoint();

        if (plugins.containsKey(id)) {
            return;
        }
        if (!preparedPlugins.contains(id)) {
            throw new IllegalStateException("Plugin was not prepared: " + id);
        }

        File jarFile = container.getPluginFile()
                .orElseThrow(() -> new IllegalStateException("Cannot load user plugin '" + id + "' without a physical JAR file"));

        long startTime = System.nanoTime();
        Plugin instance = null;

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
                ? new PluginData(jarFile, container.iconURI(), instance, meta)
                : container;

        plugins.put(id, finalizedContainer);
        loadedPluginOrder.add(id);
        cachedPlugins = Map.of();

        long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
        String pluginType = (entrypoint != null && !entrypoint.isBlank()) ? "plugin" : "library";
        LOGGER.info("Loaded {} [{}] ({}@{}) in {}ms", pluginType, meta.name(), id, meta.version(), elapsedMillis);
    }

    /**
     * Shuts down loaded plugins in reverse load order and clears plugin manager state.
     */
    @Override
    public synchronized void close() {
        if (closed) {
            return;
        }
        closed = true;

        for (int i = loadedPluginOrder.size() - 1; i >= 0; i--) {
            String pluginId = loadedPluginOrder.get(i);
            PluginData pluginData = plugins.get(pluginId);
            if (pluginData == null) {
                continue;
            }

            pluginData.getPluginInstance().ifPresent(plugin -> {
                try {
                    plugin.onShutdown();
                } catch (Throwable throwable) {
                    LOGGER.error("Failed to shut down plugin [{}]", pluginId, throwable);
                }
            });
        }

        plugins.clear();
        corePlugins.clear();
        preparedPlugins.clear();
        loadedPluginOrder.clear();
        cachedPlugins = Map.of();

        LOGGER.info("Plugin manager closed.");
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
     * Returns the results of the most recent plugin load attempt.
     *
     * @return immutable list of load results
     */
    @Override
    public List<PluginLoadResult> getLoadResults() {
        return List.copyOf(loadResults);
    }

    /**
     * Computes the execution order of candidate plugins using topological sorting.
     *
     * @param candidates list of candidate metadata descriptors
     * @return ordered list of user plugin metadata ready for sequential initialization
     * @throws IllegalStateException if missing dependencies, constraint violations, or cycles occur
     */
    @Override
    public List<Metadata> resolvePluginLoadOrder(List<Metadata> candidates) {
        return new DependencyResolver(corePlugins, config.strict()).resolve(candidates);
    }

    /**
     * Checks whether a plugin is excluded by the disabled plugin list.
     *
     * @param disabledPlugins normalized disabled entries
     * @param metadata        parsed plugin metadata
     * @param pluginFile      plugin archive
     * @return {@code true} if the plugin ID or archive name is disabled
     */
    private static boolean isPluginDisabled(Set<String> disabledPlugins, Metadata metadata, File pluginFile) {
        return disabledPlugins.contains(metadata.id().toLowerCase(Locale.ROOT))
                || disabledPlugins.contains(pluginFile.getName().toLowerCase(Locale.ROOT));
    }

    /**
     * Logs a concise summary of the most recent plugin load attempt.
     */
    private void logLoadSummary() {
        long loaded = loadResults.stream()
                .filter(result -> result.status() == PluginLoadStatus.LOADED)
                .count();
        long failed = loadResults.stream()
                .filter(result -> result.status() == PluginLoadStatus.FAILED)
                .count();
        long skipped = loadResults.stream()
                .filter(result -> result.status() == PluginLoadStatus.SKIPPED)
                .count();

        LOGGER.info("Plugin load report: {} loaded, {} failed, {} skipped", loaded, failed, skipped);

        for (PluginLoadResult result : loadResults) {
            if (result.status() == PluginLoadStatus.FAILED) {
                LOGGER.warn("Plugin [{}] failed: {}", result.pluginId(), result.message());
            }
        }
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
            return new URI("jar:" + file.toURI() + "!/" + config.pluginIconName());
        } catch (URISyntaxException _) {
            return null;
        }
    }

}