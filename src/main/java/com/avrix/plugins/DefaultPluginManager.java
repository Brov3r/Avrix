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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
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
    private final PluginApprovalService approvalService;
    private final Map<String, PluginData> plugins = new ConcurrentHashMap<>();
    private final List<Metadata> corePlugins = new CopyOnWriteArrayList<>();
    private final List<PluginLoadResult> loadResults = new CopyOnWriteArrayList<>();
    private final Set<String> attachedPlugins = ConcurrentHashMap.newKeySet();
    private final Set<String> mixinsRegisteredPlugins = ConcurrentHashMap.newKeySet();
    private final Set<String> iconlessPlugins = ConcurrentHashMap.newKeySet();
    private final Set<String> preparedPlugins = ConcurrentHashMap.newKeySet();
    private final Map<String, Object> pluginLocks = new ConcurrentHashMap<>();
    private final List<String> loadedPluginOrder = new CopyOnWriteArrayList<>();
    private final AtomicBoolean loadAllInProgress = new AtomicBoolean();
    private final Object stateLock = new Object();
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
        this(config, classLoader, gameProvider, loaderProvider, mixinRegistrar, pluginScanner, manifestParser, null);
    }

    /**
     * Constructs a new plugin manager with an optional Workshop plugin approval service.
     *
     * @param config          loader configuration
     * @param classLoader     flat classloader used to attach plugins and resolve classes
     * @param gameProvider    active game provider
     * @param loaderProvider  loader metadata provider
     * @param mixinRegistrar  registrar used for plugin mixin classes
     * @param pluginScanner   scanner used to discover plugin archives
     * @param manifestParser  parser used to read plugin manifests
     * @param approvalService approval service, or {@code null} to load every discovered archive
     * @throws NullPointerException if any required dependency is null
     */
    public DefaultPluginManager(
            LoaderConfig config,
            KnotClassLoader classLoader,
            GameProvider gameProvider,
            LoaderProvider loaderProvider,
            MixinRegistrar mixinRegistrar,
            PluginScanner pluginScanner,
            PluginManifestParser manifestParser,
            PluginApprovalService approvalService
    ) {
        this.config = Objects.requireNonNull(config, "Loader config cannot be null");
        this.classLoader = Objects.requireNonNull(classLoader, "KnotClassLoader cannot be null");
        this.gameProvider = Objects.requireNonNull(gameProvider, "GameProvider cannot be null");
        this.loaderProvider = Objects.requireNonNull(loaderProvider, "LoaderProvider cannot be null");
        this.mixinRegistrar = Objects.requireNonNull(mixinRegistrar, "Mixin registrar cannot be null");
        this.pluginScanner = Objects.requireNonNull(pluginScanner, "Plugin scanner cannot be null");
        this.manifestParser = Objects.requireNonNull(manifestParser, "Plugin manifest parser cannot be null");
        this.approvalService = approvalService;
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

        // Build descriptors before publishing them so a failure cannot leave a partial core list behind.
        List<Metadata> descriptors = List.of(gameProvider.getMetadata(), loaderProvider.getMetadata());
        corePlugins.clear();
        corePlugins.addAll(descriptors);

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
        if (!loadAllInProgress.compareAndSet(false, true)) {
            throw new IllegalStateException("Plugin loading is already in progress");
        }
        try {
            loadPluginsInternal();
        } finally {
            loadAllInProgress.set(false);
        }
    }

    private void loadPluginsInternal() {
        if (closed) {
            throw new IllegalStateException("PluginManager is closed");
        }
        if (!initialized) {
            throw new IllegalStateException("PluginManager is not initialized. Call init() first.");
        }

        loadResults.clear();
        Environment activeEnvironment = config.environment();
        Set<String> disabledPlugins = pluginScanner.readDisabledPlugins();

        List<PluginArchive> pluginArchives = pluginScanner.scanArchives();
        if (pluginArchives.isEmpty()) {
            LOGGER.debug("No plugin archives discovered in [{}]", config.pluginsDirectory());
            if (approvalService != null) {
                approvalService.flushPending();
            }
            return;
        }

        List<Metadata> candidates = new ArrayList<>(pluginArchives.size());
        Map<String, File> candidatesFiles = new HashMap<>();
        Map<String, List<String>> candidatesClassEntries = new HashMap<>();
        Map<String, Boolean> candidatesMultiRelease = new HashMap<>();
        Map<String, Boolean> candidatesIconPresent = new HashMap<>();

        for (ParsedPlugin parsedPlugin : parsePluginMetadata(pluginArchives, disabledPlugins)) {
            PluginArchive archive = parsedPlugin.archive();
            File pluginFile = archive.file();
            String pluginFileName = pluginFile.getName();

            if (parsedPlugin.disabledByFileName()) {
                LOGGER.debug("Skipping plugin archive [{}] because it is listed in {}",
                        pluginFileName, Constants.DISABLED_PLUGINS_FILE_NAME);
                loadResults.add(PluginLoadResult.skipped(pluginFileName, "Disabled by " + Constants.DISABLED_PLUGINS_FILE_NAME));
                continue;
            }

            if (parsedPlugin.failure() != null) {
                Throwable throwable = parsedPlugin.failure();
                LOGGER.warn("Failed to parse metadata from plugin archive [{}]. Skipping. Reason: {}",
                        pluginFileName, throwable.getMessage());
                loadResults.add(PluginLoadResult.failed(pluginFileName, throwable));

                if (config.strict()) {
                    throw new IllegalStateException("Failed to parse plugin metadata: " + pluginFileName, throwable);
                }
                continue;
            }

            Metadata metadata = parsedPlugin.metadata();

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

            if (approvalService != null) {
                PluginApprovalService.Status approvalStatus = approvalService.evaluate(archive, metadata);
                if (approvalStatus != PluginApprovalService.Status.APPROVED) {
                    String reason = approvalStatus == PluginApprovalService.Status.PENDING
                            ? "Pending approval"
                            : "Denied by plugin policy";
                    LOGGER.info("Skipping plugin [{}] from mod [{}]: {}", metadata.id(), archive.modId(), reason);
                    loadResults.add(PluginLoadResult.skipped(metadata.id(), reason));
                    continue;
                }
            }

            String pluginId = metadata.id();
            File existingFile = candidatesFiles.get(pluginId);

            if (existingFile != null && isPluginsDirectoryArchive(existingFile) != isPluginsDirectoryArchive(pluginFile)) {
                if (isPluginsDirectoryArchive(existingFile)) {
                    LOGGER.info(
                            "Duplicate plugin ID [{}]: replacing plugins-directory archive [{}] with mod archive [{}]",
                            pluginId,
                            existingFile,
                            pluginFile
                    );
                    candidates.removeIf(candidate -> candidate.id().equals(pluginId));
                    candidates.add(metadata);
                    candidatesFiles.put(pluginId, pluginFile);
                    candidatesClassEntries.put(pluginId, parsedPlugin.classEntries());
                    candidatesMultiRelease.put(pluginId, parsedPlugin.multiRelease());
                    candidatesIconPresent.put(pluginId, parsedPlugin.iconPresent());
                } else {
                    LOGGER.warn(
                            "Duplicate plugin ID [{}]: keeping mod archive [{}], ignoring plugins-directory archive [{}]",
                            pluginId,
                            existingFile,
                            pluginFile
                    );
                }
                continue;
            }

            candidates.add(metadata);
            candidatesFiles.put(pluginId, pluginFile);
            candidatesClassEntries.put(pluginId, parsedPlugin.classEntries());
            candidatesMultiRelease.put(pluginId, parsedPlugin.multiRelease());
            candidatesIconPresent.put(pluginId, parsedPlugin.iconPresent());
            LOGGER.debug("Candidate plugin discovered: [{}] ({}@{})", metadata.name(), pluginId, metadata.version());
        }

        if (approvalService != null) {
            approvalService.flushPending();
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

        List<PluginData> attachedContainers = new ArrayList<>(sortedPlugins.size());
        for (Metadata meta : sortedPlugins) {
            if (corePlugins.stream().anyMatch(core -> core.id().equals(meta.id()))) {
                continue;
            }

            File pluginFile = candidatesFiles.get(meta.id());
            boolean iconPresent = Boolean.TRUE.equals(candidatesIconPresent.get(meta.id()));
            if (!iconPresent) {
                iconlessPlugins.add(meta.id());
            }
            PluginData container = new PluginData(
                    pluginFile,
                    buildIconUri(pluginFile, iconPresent),
                    null,
                    meta
            );

            try {
                synchronized (pluginLock(meta.id())) {
                    if (closed) {
                        throw new IllegalStateException("PluginManager is closed");
                    }
                    if (attachPlugin(
                            container,
                            candidatesClassEntries.get(meta.id()),
                            Boolean.TRUE.equals(candidatesMultiRelease.get(meta.id()))
                    )) {
                        attachedContainers.add(container);
                    }
                }
            } catch (Throwable throwable) {
                LOGGER.error("Failed to attach plugin [{}]. Skipping execution.", meta.name(), throwable);
                loadResults.add(PluginLoadResult.failed(meta.id(), throwable));

                if (config.strict()) {
                    throw new IllegalStateException("Plugin preparation failed: " + meta.id(), throwable);
                }
            }
        }

        List<PluginData> preparedContainers = new ArrayList<>(attachedContainers.size());
        for (PluginData container : attachedContainers) {
            String pluginId = container.metadata().id();
            try {
                synchronized (pluginLock(pluginId)) {
                    if (closed) {
                        throw new IllegalStateException("PluginManager is closed");
                    }
                    if (!attachedPlugins.contains(pluginId)) {
                        continue;
                    }
                    registerMixins(container);
                    preparedPlugins.add(pluginId);
                    preparedContainers.add(container);
                }
            } catch (Throwable throwable) {
                LOGGER.error("Failed to prepare plugin [{}]. Skipping execution.", container.metadata().name(), throwable);
                loadResults.add(PluginLoadResult.failed(pluginId, throwable));

                if (config.strict()) {
                    throw new IllegalStateException("Plugin preparation failed: " + pluginId, throwable);
                }
            }
        }

        for (PluginData container : preparedContainers) {
            String pluginId = container.metadata().id();
            try {
                synchronized (pluginLock(pluginId)) {
                    if (closed) {
                        throw new IllegalStateException("PluginManager is closed");
                    }
                    if (startPlugin(container)) {
                        loadResults.add(PluginLoadResult.loaded(pluginId));
                    }
                }
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
     * Parses plugin manifests in parallel while preserving the original archive order.
     *
     * @param pluginArchives  discovered plugin archives
     * @param disabledPlugins normalized disabled plugin entries
     * @return parse results aligned with {@code pluginArchives}
     */
    private List<ParsedPlugin> parsePluginMetadata(
            List<PluginArchive> pluginArchives,
            Set<String> disabledPlugins
    ) {
        int threadCount = Math.min(16, Math.max(2, Runtime.getRuntime().availableProcessors()));
        List<ParsedPlugin> results = new ArrayList<>(pluginArchives.size());

        try (ExecutorService executor = Executors.newFixedThreadPool(threadCount)) {
            List<Future<ParsedPlugin>> futures = new ArrayList<>(pluginArchives.size());
            for (PluginArchive archive : pluginArchives) {
                futures.add(executor.submit(() -> parsePlugin(archive, disabledPlugins)));
            }

            for (int i = 0; i < futures.size(); i++) {
                try {
                    results.add(futures.get(i).get());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Interrupted while parsing plugin metadata", e);
                } catch (ExecutionException e) {
                    Throwable cause = (e.getCause() != null) ? e.getCause() : e;
                    results.add(ParsedPlugin.failed(pluginArchives.get(i), cause));
                }
            }
        }

        return results;
    }

    /**
     * Parses a single plugin manifest or returns a skipped/failed result.
     *
     * @param archive         plugin archive with source metadata
     * @param disabledPlugins normalized disabled plugin entries
     * @return parsed plugin result
     */
    private ParsedPlugin parsePlugin(PluginArchive archive, Set<String> disabledPlugins) {
        File pluginFile = archive.file();
        String pluginFileName = pluginFile.getName();
        if (disabledPlugins.contains(pluginFileName.toLowerCase(Locale.ROOT))) {
            return ParsedPlugin.disabled(archive);
        }

        try {
            PluginManifestParser.ParsedJarManifest manifest = manifestParser.fromJarFileWithClassEntries(
                    pluginFile.toPath(),
                    config.metadataFileName(),
                    config.pluginIconName()
            );
            return ParsedPlugin.parsed(
                    archive,
                    manifest.metadata(),
                    manifest.classEntries(),
                    manifest.multiRelease(),
                    manifest.iconPresent()
            );
        } catch (Throwable throwable) {
            return ParsedPlugin.failed(archive, throwable);
        }
    }

    /**
     * Result of parsing one plugin manifest before environment and duplicate checks.
     *
     * @param archive              plugin archive with source metadata
     * @param metadata             parsed metadata, or {@code null} for skipped/failed entries
     * @param classEntries         class resource paths from the same archive
     * @param multiRelease         whether the archive is a multi-release JAR
     * @param iconPresent          whether the archive contains the configured icon
     * @param failure              parsing failure, or {@code null}
     * @param disabledByFileName   whether the archive name is listed in the disabled plugin file
     */
    private record ParsedPlugin(
            PluginArchive archive,
            Metadata metadata,
            List<String> classEntries,
            boolean multiRelease,
            boolean iconPresent,
            Throwable failure,
            boolean disabledByFileName
    ) {

        private static ParsedPlugin disabled(PluginArchive archive) {
            return new ParsedPlugin(archive, null, List.of(), false, false, null, true);
        }

        private static ParsedPlugin parsed(
                PluginArchive archive,
                Metadata metadata,
                List<String> classEntries,
                boolean multiRelease,
                boolean iconPresent
        ) {
            return new ParsedPlugin(archive, metadata, classEntries, multiRelease, iconPresent, null, false);
        }

        private static ParsedPlugin failed(PluginArchive archive, Throwable failure) {
            return new ParsedPlugin(archive, null, List.of(), false, false, failure, false);
        }
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

        if (!meta.environment().isCompatibleWith(config.environment())) {
            throw new IllegalStateException(
                    "Plugin [%s] is not compatible with environment [%s]"
                            .formatted(id, config.environment())
            );
        }

        container.getPluginFile()
                .orElseThrow(() -> new IllegalStateException(
                        "Cannot load user plugin '" + id + "' without a physical JAR file"
                ));

        synchronized (pluginLock(id)) {
            if (closed) {
                throw new IllegalStateException("PluginManager is closed");
            }
            if (plugins.containsKey(id)) {
                LOGGER.debug("Plugin [{}] is already loaded. Skipping duplicate load request.", id);
                return;
            }

            try {
                if (!preparePlugin(container) || !startPlugin(container)) {
                    return;
                }
                loadResults.add(PluginLoadResult.loaded(id));
            } catch (Throwable throwable) {
                loadResults.add(PluginLoadResult.failed(id, throwable));
                LOGGER.error("Failed to load plugin [{}]: {}", id, throwable.getMessage(), throwable);
                throw new RuntimeException("Failed to load plugin: " + id, throwable);
            }
        }
    }

    /**
     * Attaches a plugin archive, registers its mixins, and marks it ready for startup.
     *
     * @param container plugin metadata and file container
     * @return {@code true} if the plugin is ready for startup, {@code false} if it is already loaded
     * @throws MalformedURLException if the plugin archive cannot be converted to a URL
     */
    private boolean preparePlugin(PluginData container) throws MalformedURLException {
        return preparePlugin(container, null);
    }

    /**
     * Attaches a plugin archive, registers its mixins, and marks it ready for startup.
     *
     * @param container    plugin metadata and file container
     * @param classEntries class entries collected while parsing the manifest, or {@code null} when unavailable
     * @return {@code true} if the plugin is ready for startup, {@code false} if it is already loaded
     * @throws IllegalStateException if the plugin has no physical JAR file
     * @throws MalformedURLException if the plugin archive cannot be converted to a URL
     */
    private boolean preparePlugin(PluginData container, List<String> classEntries) throws MalformedURLException {
        String id = container.metadata().id();

        if (plugins.containsKey(id)) {
            return false;
        }
        if (preparedPlugins.contains(id)) {
            return true;
        }

        if (!attachPlugin(container, classEntries, null)) {
            return false;
        }
        registerMixins(container);
        preparedPlugins.add(id);
        LOGGER.debug("Prepared plugin [{}] ({})", container.metadata().name(), id);
        return true;
    }

    /**
     * Attaches a plugin archive to the flat classloader without registering mixins.
     *
     * @param container    plugin metadata and file container
     * @param classEntries class entries collected while parsing the manifest, or {@code null} when unavailable
     * @return {@code true} if the archive is attached, {@code false} if the plugin is already loaded
     * @throws IllegalStateException if the plugin has no physical JAR file
     * @throws MalformedURLException if the plugin archive cannot be converted to a URL
     */
    private boolean attachPlugin(PluginData container, List<String> classEntries) throws MalformedURLException {
        return attachPlugin(container, classEntries, null);
    }

    /**
     * Attaches a plugin archive using optionally pre-computed archive flags.
     *
     * @param container    plugin metadata and file container
     * @param classEntries class entries collected while parsing the manifest, or {@code null} when unavailable
     * @param multiRelease known multi-release flag, or {@code null} when it must be detected
     * @return {@code true} if the archive is attached, {@code false} if the plugin is already loaded
     * @throws IllegalStateException if the plugin has no physical JAR file
     * @throws MalformedURLException if the plugin archive cannot be converted to a URL
     */
    private boolean attachPlugin(
            PluginData container,
            List<String> classEntries,
            Boolean multiRelease
    ) throws MalformedURLException {
        Metadata meta = container.metadata();
        String id = meta.id();

        if (plugins.containsKey(id)) {
            return false;
        }
        if (attachedPlugins.contains(id)) {
            return true;
        }

        File jarFile = container.getPluginFile()
                .orElseThrow(() -> new IllegalStateException("Cannot load user plugin '" + id + "' without a physical JAR file"));

        if (!attachedPlugins.add(id)) {
            return true;
        }

        try {
            URL jarUrl = jarFile.toURI().toURL();
            if (classEntries != null && multiRelease != null) {
                if (classEntries.isEmpty()) {
                    classLoader.addURL(jarUrl, false);
                } else {
                    classLoader.addURL(jarUrl, true, classEntries, multiRelease);
                }
            } else {
                classLoader.addURL(jarUrl, !meta.mixins().isEmpty());
            }
        } catch (MalformedURLException | RuntimeException e) {
            attachedPlugins.remove(id);
            throw e;
        }

        LOGGER.debug("Attached plugin archive [{}] ({})", meta.name(), id);
        return true;
    }

    /**
     * Registers all declared mixins for an attached plugin.
     *
     * @param container plugin metadata and file container
     */
    private void registerMixins(PluginData container) {
        Metadata meta = container.metadata();
        String id = meta.id();

        if (mixinsRegisteredPlugins.contains(id)) {
            return;
        }

        if (!meta.mixins().isEmpty()) {
            for (String mixin : meta.mixins()) {
                mixinRegistrar.addMixin(mixin);
            }
            LOGGER.debug("Registered {} mixin configuration(s) for plugin [{}]", meta.mixins().size(), id);
        }
        mixinsRegisteredPlugins.add(id);
    }

    /**
     * Starts a prepared plugin by creating its entrypoint and invoking {@link Plugin#onInitialize(PluginData)}.
     *
     * @param container plugin metadata and file container
     * @throws IllegalStateException  if the plugin was not prepared or cannot be started
     * @throws ReflectiveOperationException if the entrypoint cannot be loaded or instantiated
     */
    private boolean startPlugin(PluginData container) throws ReflectiveOperationException {
        Metadata meta = container.metadata();
        String id = meta.id();
        String entrypoint = meta.entrypoint();

        if (!preparedPlugins.contains(id)) {
            throw new IllegalStateException("Plugin was not prepared: " + id);
        }

        File jarFile = container.getPluginFile()
                .orElseThrow(() -> new IllegalStateException("Cannot load user plugin '" + id + "' without a physical JAR file"));

        synchronized (stateLock) {
            if (closed || plugins.containsKey(id)) {
                return false;
            }
        }

        long startTime = System.nanoTime();

        URI iconUri = container.iconURI();
        if (iconUri == null && !iconlessPlugins.contains(id)) {
            iconUri = buildIconUri(jarFile);
            if (iconUri == null) {
                iconlessPlugins.add(id);
            }
        }
        Plugin instance = null;
        if (entrypoint != null && !entrypoint.isBlank()) {
            Class<?> pluginClass = Class.forName(entrypoint, false, classLoader);

            if (!Plugin.class.isAssignableFrom(pluginClass)) {
                throw new IllegalArgumentException("Declared entrypoint [" + entrypoint + "] does not implement Plugin interface");
            }

            instance = (Plugin) pluginClass.getDeclaredConstructor().newInstance();
        }

        PluginData finalizedContainer = new PluginData(jarFile, iconUri, instance, meta);
        if (instance != null) {
            instance.onInitialize(finalizedContainer);
            LOGGER.debug("Initialized entrypoint [{}] for plugin [{}]", entrypoint, id);
        }

        boolean closedDuringStart;
        synchronized (stateLock) {
            closedDuringStart = closed;
            if (!closedDuringStart) {
                plugins.put(id, finalizedContainer);
                loadedPluginOrder.add(id);
                cachedPlugins = Map.of();
            }
        }

        if (closedDuringStart) {
            shutdownQuietly(id, instance);
            return false;
        }

        long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
        String pluginType = (entrypoint != null && !entrypoint.isBlank()) ? "plugin" : "library";
        LOGGER.info("Loaded {} [{}] ({}@{}) in {}ms", pluginType, meta.name(), id, meta.version(), elapsedMillis);
        return true;
    }

    /**
     * Shuts down a plugin instance without allowing its failure to escape.
     *
     * @param pluginId plugin identifier used for logging
     * @param instance plugin instance, may be {@code null}
     */
    private static void shutdownQuietly(String pluginId, Plugin instance) {
        if (instance == null) {
            return;
        }
        try {
            instance.onShutdown();
        } catch (Throwable throwable) {
            LOGGER.error("Failed to shut down plugin [{}]", pluginId, throwable);
        }
    }

    /**
     * Shuts down loaded plugins in reverse load order and clears plugin manager state.
     */
    @Override
    public void close() {
        List<String> pluginOrder;
        synchronized (stateLock) {
            if (closed) {
                return;
            }
            closed = true;
            pluginOrder = new ArrayList<>(loadedPluginOrder);
        }

        for (int i = pluginOrder.size() - 1; i >= 0; i--) {
            String pluginId = pluginOrder.get(i);
            PluginData pluginData = plugins.get(pluginId);
            if (pluginData == null) {
                continue;
            }
            pluginData.getPluginInstance().ifPresent(plugin -> shutdownQuietly(pluginId, plugin));
        }

        synchronized (stateLock) {
            plugins.clear();
            corePlugins.clear();
            attachedPlugins.clear();
            mixinsRegisteredPlugins.clear();
            iconlessPlugins.clear();
            preparedPlugins.clear();
            pluginLocks.clear();
            loadedPluginOrder.clear();
            cachedPlugins = Map.of();
        }

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
     * Returns the per-plugin lifecycle lock.
     *
     * @param pluginId plugin identifier
     * @return lock object dedicated to the specified plugin
     */
    private Object pluginLock(String pluginId) {
        return pluginLocks.computeIfAbsent(pluginId, ignored -> new Object());
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
     * Checks whether an archive belongs to the manually managed plugins directory.
     *
     * @param pluginFile plugin archive
     * @return {@code true} when the archive is located under the configured plugins directory
     */
    private boolean isPluginsDirectoryArchive(File pluginFile) {
        Path pluginsDirectory = config.pluginsDirectory().toAbsolutePath().normalize();
        Path file = pluginFile.toPath().toAbsolutePath().normalize();

        try {
            return file.toRealPath().startsWith(pluginsDirectory.toRealPath());
        } catch (IOException e) {
            return file.startsWith(pluginsDirectory);
        }
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
            if (!PluginResourceCache.containsEntry(file.toPath(), config.pluginIconName())) {
                return null;
            }
        } catch (IOException e) {
            LOGGER.debug("Failed to inspect plugin icon in [{}]", file.getName(), e);
            return null;
        }

        return buildIconUri(file, true);
    }

    /**
     * Builds an icon URI when the caller already knows whether the archive contains the icon.
     *
     * @param file        plugin archive
     * @param iconPresent whether the archive contains the configured icon
     * @return icon URI, or {@code null} when the icon is absent or the URI cannot be built
     */
    private URI buildIconUri(File file, boolean iconPresent) {
        if (file == null || !iconPresent) {
            return null;
        }

        try {
            return new URI("jar:" + file.toURI() + "!/" + config.pluginIconName());
        } catch (URISyntaxException _) {
            return null;
        }
    }

}