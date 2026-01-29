package com.avrix;

import com.avrix.enums.Environment;
import com.avrix.enums.PluginType;
import com.avrix.loaders.AvrixClassLoader;
import com.avrix.plugins.Metadata;
import com.avrix.plugins.PluginManager;
import com.avrix.provider.GameProvider;
import com.avrix.provider.ZomboidGameProvider;
import com.avrix.utils.Constants;
import org.tinylog.Logger;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.stream.Stream;

/**
 * Bootstraps the Avrix runtime.
 */
public final class LoaderManager {
    private static volatile AvrixClassLoader avrixClassLoader;

    private static boolean initialized;
    private static boolean launched;
    private static GameProvider provider;

    private LoaderManager() {
        // Utility class
    }

    /**
     * Initializes the runtime.
     *
     * @throws LoaderException if any step of initialization fails
     */
    public static synchronized void initialize() throws LoaderException {
        if (initialized) {
            Logger.debug("LoaderManager already initialized. Skipping.");
            return;
        }

        Logger.info("Initializing LoaderManager...");

        try {
            if (avrixClassLoader == null) {
                Path rootDir = getRootDirectory();
                Logger.debug("Resolved root directory: '{}'", rootDir);

                URL[] urls = findRootJars(rootDir);
                Logger.info("Discovered {} root JAR(s).", urls.length);

                avrixClassLoader = new AvrixClassLoader(urls, LoaderManager.class.getClassLoader());
            }

            registerLoaderMetadata();

            provider = loadGameProvider(avrixClassLoader);

            PluginManager.initialize(avrixClassLoader);

            initialized = true;
            Logger.info("LoaderManager initialized successfully. Provider='{}', Environment={}",
                    provider.getId(), provider.getEnvironment());

        } catch (Exception e) {
            initialized = false;
            launched = false;
            provider = null;

            Logger.error("LoaderManager initialization failed.", e);
            throw new LoaderException("LoaderManager initialization failed", e);
        }
    }

    /**
     * Launches plugins and then starts the selected {@link GameProvider}.
     *
     * <p>
     * Requires prior successful {@link #initialize()}.
     * </p>
     *
     * @param args command-line arguments; may be {@code null}
     * @throws ProviderLaunchException if plugin launch or provider launch fails
     * @throws IllegalStateException   if called before initialization
     */
    public static synchronized void launch(String[] args) throws ProviderLaunchException {
        ensureInitialized();

        if (launched) {
            Logger.debug("LoaderManager already launched. Skipping.");
            return;
        }

        try {
            PluginManager.launch();

            String[] safeArgs = (args == null) ? new String[0] : args;

            provider.launch(safeArgs);

            launched = true;
        } catch (Exception e) {
            Logger.error("Failed to launch runtime.", e);
            throw new ProviderLaunchException("Failed to launch runtime", e);
        }
    }

    /**
     * Returns the selected provider.
     *
     * @return selected {@link GameProvider}
     * @throws IllegalStateException if called before initialization
     */
    public static GameProvider getProvider() {
        ensureInitialized();
        return provider;
    }

    /**
     * Returns the core classloader used by the runtime.
     *
     * @return {@link AvrixClassLoader}
     * @throws IllegalStateException if called before initialization
     */
    public static AvrixClassLoader getClassLoader() {
        ensureInitialized();
        return avrixClassLoader;
    }

    /**
     * Returns whether the selected provider indicates server mode.
     *
     * @return {@code true} if server mode, otherwise {@code false}
     * @throws IllegalStateException if called before initialization
     */
    public static boolean isServerMode() {
        ensureInitialized();
        return provider.isServer();
    }

    /**
     * Registers loader metadata in the {@link PluginManager}.
     */
    private static void registerLoaderMetadata() {
        Metadata metadata = new Metadata.Builder()
                .name(Constants.LOADER_NAME)
                .id(Constants.LOADER_ID)
                .author(Constants.LOADER_AUTHOR)
                .environment(Environment.BOTH.getValue())
                .version(Constants.LOADER_VERSION)
                .type(PluginType.LOADER)
                .license(Constants.LOADER_LICENSE)
                .contacts(Constants.LOADER_CONTACTS)
                .entryPoints(List.of())
                .build();

        PluginManager.registerLoader(metadata);
        Logger.debug("Loader registered in PluginManager: '{}' (ID: {})", Constants.LOADER_NAME, Constants.LOADER_ID);
    }

    /**
     * Discovers JAR files located directly in the given root directory (depth 1).
     *
     * @param rootDir root directory
     * @return array of jar URLs (possibly empty)
     * @throws NullPointerException if {@code rootDir} is {@code null}
     * @throws Exception            if directory cannot be listed
     */
    private static URL[] findRootJars(Path rootDir) throws Exception {
        Objects.requireNonNull(rootDir, "Root directory must not be null");

        try (Stream<Path> stream = Files.list(rootDir)) {
            List<URL> urls = stream
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(Constants.JAR_EXTENSION))
                    .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                    .map(p -> {
                        try {
                            return p.toUri().toURL();
                        } catch (Exception e) {
                            throw new IllegalStateException("Failed to convert path to URL: " + p, e);
                        }
                    })
                    .toList();

            Logger.debug("Root JARs: {}", urls.stream()
                    .map(LoaderManager::safeUrlDisplayName)
                    .toList());

            if (urls.size() > 30) {
                Logger.debug("Root JAR list truncated. Total={}", urls.size());
            }

            return urls.toArray(URL[]::new);
        }
    }

    /**
     * Returns a human-readable display name for the given {@link URL}.
     *
     * <p>
     * If the URL cannot be converted to a {@link java.nio.file.Path} (for example,
     * due to invalid syntax or an unsupported scheme), this method falls back to
     * {@link URL#toExternalForm()} to ensure that logging never fails.
     * </p>
     *
     * @param url the URL to format; must not be {@code null}
     * @return a human-readable name suitable for logs, never {@code null}
     * @throws NullPointerException if {@code url} is {@code null}
     */
    private static String safeUrlDisplayName(URL url) {
        try {
            return Paths.get(url.toURI()).getFileName().toString();
        } catch (Exception ignored) {
            // Best-effort display name. Intentionally ignored.
            return url.toExternalForm();
        }
    }

    /**
     * Discovers and initializes a single {@link GameProvider}.
     *
     * <p>
     * Provider selection rules are implemented by {@link #selectProvider(List)}.
     * After selection, {@link GameProvider#initialize(AvrixClassLoader)} is invoked,
     * followed by {@link GameProvider#redirectSystemStreamsToLogger()}.
     * Provider metadata is then registered in {@link PluginManager}.
     * </p>
     *
     * @param loader core classloader
     * @return initialized provider
     */
    private static GameProvider loadGameProvider(AvrixClassLoader loader) throws Exception {
        Objects.requireNonNull(loader, "CoreClassLoader must not be null");

        Logger.info("Discovering GameProvider(s) via ServiceLoader...");

        ServiceLoader<GameProvider> serviceLoader = ServiceLoader.load(GameProvider.class, loader);
        List<GameProvider> allProviders = serviceLoader.stream()
                .map(ServiceLoader.Provider::get)
                .toList();

        if (allProviders.isEmpty()) {
            throw new IllegalStateException("No GameProvider found in classpath.");
        }

        GameProvider selected = selectProvider(allProviders);
        Logger.info("Selected GameProvider implementation: {}", selected.getClass().getName());

        selected.initialize(loader);
        selected.redirectSystemStreamsToLogger();

        PluginManager.registerProvider(selected.getMetadata());
        Logger.debug("GameProvider registered in PluginManager: '{}' (ID: {})", selected.getGameName(), selected.getId());

        return selected;
    }

    /**
     * Selects a single provider from the discovered list.
     *
     * <p>
     * Rule:
     * </p>
     * <ul>
     *   <li>Prefer a single external provider (not {@link ZomboidGameProvider}).</li>
     *   <li>If no external providers exist, use the first discovered provider as a fallback.</li>
     *   <li>If multiple external providers exist, fail fast with a clear error.</li>
     * </ul>
     *
     * @param allProviders list of all discovered providers
     * @return selected provider
     * @throws NullPointerException     if {@code allProviders} is {@code null}
     * @throws IllegalArgumentException if {@code allProviders} is empty
     * @throws IllegalStateException    if multiple external providers are present
     */
    static GameProvider selectProvider(List<GameProvider> allProviders) {
        Objects.requireNonNull(allProviders, "Providers list must not be null");
        if (allProviders.isEmpty()) {
            throw new IllegalArgumentException("Providers list must not be empty.");
        }

        List<GameProvider> externalProviders = allProviders.stream()
                .filter(p -> !(p instanceof ZomboidGameProvider))
                .toList();

        if (externalProviders.isEmpty()) {
            GameProvider fallback = allProviders.getFirst();
            Logger.debug("No external GameProvider found; using default: {}", fallback.getClass().getName());
            return fallback;
        }

        if (externalProviders.size() == 1) {
            return externalProviders.getFirst();
        }

        throw new IllegalStateException(
                "Multiple external GameProviders found. Please remove extras: " +
                        externalProviders.stream().map(p -> p.getClass().getName()).toList()
        );
    }

    /**
     * Resolves the root directory of the loader distribution.
     *
     * <p>
     * The root directory is derived from the location of the {@link LoaderManager} code source:
     * parent directory of the loader JAR (or classes directory).
     * </p>
     *
     * @return root directory path
     * @throws IllegalStateException if the code source cannot be resolved
     */
    private static Path getRootDirectory() {
        try {
            CodeSource codeSource = LoaderManager.class.getProtectionDomain().getCodeSource();
            if (codeSource == null) {
                throw new IllegalStateException("CodeSource is null for LoaderManager.");
            }

            Path jarOrDir = Paths.get(codeSource.getLocation().toURI()).toAbsolutePath().normalize();
            Path parent = jarOrDir.getParent();
            if (parent == null) {
                throw new IllegalStateException("Cannot resolve root directory from: " + jarOrDir);
            }
            return parent;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to resolve root directory for LoaderManager.", e);
        }
    }

    /**
     * Ensures that the loader manager has been initialized.
     *
     * @throws IllegalStateException if not initialized
     */
    private static void ensureInitialized() {
        if (!initialized) {
            throw new IllegalStateException("LoaderManager is not initialized. Call initialize() first.");
        }
    }

    /**
     * Indicates a failure during loader initialization.
     */
    public static final class LoaderException extends Exception {
        public LoaderException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Indicates a failure during plugin/provider launch.
     */
    public static final class ProviderLaunchException extends Exception {
        public ProviderLaunchException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}