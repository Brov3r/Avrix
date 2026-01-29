package com.avrix.provider;

import com.avrix.enums.Environment;
import com.avrix.enums.PluginType;
import com.avrix.loaders.AvrixClassLoader;
import com.avrix.logging.LineReadingOutputStream;
import com.avrix.logging.ZomboidLogLineParser;
import com.avrix.plugins.Metadata;
import org.tinylog.Logger;

import java.io.PrintStream;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * {@link GameProvider} implementation for Project Zomboid.
 *
 * <p>
 * This provider supports both client and dedicated server distributions.
 * </p>
 */
public final class ZomboidGameProvider implements GameProvider {
    /**
     * Pattern used to extract a SemVer-like {@code major.minor.patch} triple from the raw version string.
     */
    private static final Pattern VERSION_PATTERN = Pattern.compile("(\\d+\\.\\d+\\.\\d+)");

    /**
     * Fallback version string used when the raw version is not available.
     */
    private static final String UNKNOWN_VERSION = "??.??.??";

    /**
     * Base system properties applied during initialization.
     */
    private static final Map<String, String> BASE_CORE_ARGS = Map.of(
            "zomboid.steam", "1",
            "zomboid.znetlog", "1"
    );

    /**
     * Lock guarding initialization and operations that must not run concurrently
     * (e.g. stream redirection).
     */
    private final Object initLock = new Object();

    /**
     * Lock guarding lazy cache population.
     */
    private final Object cacheLock = new Object();

    /**
     * Indicates whether the provider has been successfully initialized.
     */
    private volatile boolean initialized;

    /**
     * Classloader used to load game classes and resolve native libraries.
     * Populated during initialization.
     */
    private volatile AvrixClassLoader classLoader;

    /**
     * Entrypoint class loaded from the game classloader.
     * Populated during initialization.
     */
    private volatile Class<?> gameClass;

    /**
     * Cached raw game version; {@code null} indicates "not computed".
     */
    private volatile String rawVersionCache; // null => not computed

    /**
     * Cached normalized game version; {@code null} indicates "not computed".
     */
    private volatile String normalizedVersionCache; // null => not computed

    /**
     * Cached provider metadata; {@code null} indicates "not computed".
     */
    private volatile Metadata metadataCache; // null => not computed

    /**
     * Cached server installation detection; {@code null} indicates "not computed".
     */
    private volatile Boolean isServerInstallCache; // null => not computed

    /**
     * Cached Java libraries (classpath JARs); {@code null} indicates "not computed".
     */
    private volatile List<Path> javaLibsCache; // null => not computed

    /**
     * Cached native library search directories; {@code null} indicates "not computed".
     */
    private volatile List<Path> nativePathsCache; // null => not computed

    /**
     * Indicates whether {@link System#out} and {@link System#err} have been redirected.
     */
    private volatile boolean streamsRedirected;

    /**
     * Arguments passed at startup
     */
    private String[] launchArgs;

    /**
     * Initializes the provider before game launch.
     *
     * @param classLoader the {@link AvrixClassLoader} used for game execution; must not be {@code null}
     * @throws NullPointerException  if {@code classLoader} is {@code null}
     * @throws IllegalStateException if initialization fails
     */
    @Override
    public void initialize(AvrixClassLoader classLoader) {
        Objects.requireNonNull(classLoader, "CoreClassLoader must not be null");

        if (initialized) {
            Logger.debug("{} provider already initialized. Skipping.", getId());
            return;
        }

        synchronized (initLock) {
            Logger.info("Initializing provider '{}'", getId());

            this.classLoader = classLoader;

            Thread thread = Thread.currentThread();
            ClassLoader previous = thread.getContextClassLoader();

            try {
                thread.setContextClassLoader(classLoader);

                Map<String, String> props = getProviderCoreArgs();
                if (!props.isEmpty()) {
                    props.forEach(System::setProperty);
                    Logger.debug("Applied {} system properties for '{}'", props.size(), getId());
                }

                List<Path> natives = getNativePaths();
                if (!natives.isEmpty()) {
                    classLoader.addNativePaths(natives);
                    Logger.debug("Registered {} native search path(s) for '{}'", natives.size(), getId());
                }

                List<Path> libs = getJavaLibs();
                if (!libs.isEmpty()) {
                    for (Path jar : libs) {
                        classLoader.addURL(jar.toUri().toURL());
                    }
                    Logger.debug("Added {} classpath URL(s) for '{}'", libs.size(), getId());
                }

                String entry = getEntrypoint();
                gameClass = classLoader.loadClass(entry);

                initialized = true;
                Logger.info("Provider '{}' initialized successfully. Environment={}", getId(), getEnvironment());

            } catch (Exception e) {
                this.gameClass = null;
                this.classLoader = null;
                this.initialized = false;

                Logger.error("Failed to initialize provider '{}'", getId(), e);
                throw new IllegalStateException("Failed to initialize provider: " + getId(), e);
            } finally {
                thread.setContextClassLoader(previous);
            }
        }
    }

    /**
     * Launches the game by invoking the entrypoint {@code main(String[])} method.
     *
     * @param args command-line arguments provided by the runtime; may be {@code null}
     * @throws IllegalStateException if the provider is not initialized, is in an invalid state,
     *                               the entrypoint does not define {@code main(String[])},
     *                               or the launch fails
     */
    @Override
    public void launch(String[] args) {
        if (!initialized) {
            throw new IllegalStateException("Provider must be initialized before launch. Call initialize() first.");
        }
        if (classLoader == null || gameClass == null) {
            throw new IllegalStateException("Provider is in an invalid state: game classloader or entrypoint class is null.");
        }

        String[] safeArgs = (args == null) ? new String[0] : args;
        launchArgs = safeArgs;

        Thread thread = Thread.currentThread();
        ClassLoader previous = thread.getContextClassLoader();

        try {
            thread.setContextClassLoader(classLoader);

            Logger.info("Launching {} (env={}, version={}) via {}",
                    getGameName(), getEnvironment(), getNormalizedVersion(), getEntrypoint());

            Method main = gameClass.getDeclaredMethod("main", String[].class);
            main.setAccessible(true);
            main.invoke(null, (Object) safeArgs);

        } catch (NoSuchMethodException e) {
            Logger.error("Entrypoint '{}' does not declare main(String[]).", getEntrypoint(), e);
            throw new IllegalStateException("Entrypoint does not declare main(String[]): " + getEntrypoint(), e);
        } catch (Exception e) {
            Logger.error("Failed to launch game via provider '{}'", getId(), e);
            throw new IllegalStateException("Failed to launch game via provider: " + getId(), e);
        } finally {
            thread.setContextClassLoader(previous);
        }
    }

    /**
     * Returns the stable provider identifier.
     *
     * @return provider identifier
     */
    @Override
    public String getId() {
        return "project-zomboid";
    }

    /**
     * Returns the human-readable game name.
     *
     * @return game name, including "Server" suffix when running in server mode
     */
    @Override
    public String getGameName() {
        return isServer() ? "Project Zomboid Server" : "Project Zomboid";
    }

    /**
     * Returns the game author or vendor name.
     *
     * @return author string
     */
    @Override
    public String getGameAuthor() {
        return "The Indie Stone";
    }

    /**
     * Returns the normalized version string.
     *
     * <p>
     * If a SemVer-like triple can be extracted from {@link #getRawVersion()},
     * that triple is returned. Otherwise, the raw version is returned as-is.
     * If the raw version is not available, {@link #UNKNOWN_VERSION} is returned.
     * </p>
     *
     * @return normalized version, never {@code null}
     */
    @Override
    public String getNormalizedVersion() {
        String cached = normalizedVersionCache;
        if (cached != null) {
            return cached;
        }

        synchronized (cacheLock) {
            if (normalizedVersionCache != null) {
                return normalizedVersionCache;
            }

            String raw = getRawVersion();
            if (raw.isBlank()) {
                normalizedVersionCache = UNKNOWN_VERSION;
                Logger.debug("Raw version is empty; using '{}' for '{}'", UNKNOWN_VERSION, getId());
                return normalizedVersionCache;
            }

            Matcher matcher = VERSION_PATTERN.matcher(raw);
            normalizedVersionCache = matcher.find() ? matcher.group(1) : raw;
            return normalizedVersionCache;
        }
    }

    /**
     * Returns the raw game version string retrieved from the game runtime.
     *
     * <p>
     * The provider attempts to load {@code zombie.core.Core} and calls:
     * {@code Core.getInstance().getVersion()} via reflection. The value is cached.
     * If the version cannot be retrieved, an empty string is returned.
     * </p>
     *
     * @return raw version string, or an empty string if unavailable
     * @throws IllegalStateException if called before {@link #initialize(AvrixClassLoader)}
     */
    @Override
    public String getRawVersion() {
        String cached = rawVersionCache;
        if (cached != null) {
            return cached;
        }

        AvrixClassLoader cl = this.classLoader;
        if (cl == null) {
            throw new IllegalStateException("ClassLoader not initialized. Call initialize() first.");
        }

        synchronized (cacheLock) {
            if (rawVersionCache != null) {
                return rawVersionCache;
            }

            try {
                Class<?> coreClass = cl.loadClass("zombie.core.Core");

                Method getInstance = coreClass.getDeclaredMethod("getInstance");
                getInstance.setAccessible(true);
                Object coreInstance = getInstance.invoke(null);

                Method getVersion = coreClass.getDeclaredMethod("getVersion");
                getVersion.setAccessible(true);
                Object version = getVersion.invoke(coreInstance);

                rawVersionCache = (version instanceof String s) ? s : "";
                return rawVersionCache;

            } catch (ClassNotFoundException e) {
                Logger.warn("Cannot load 'zombie.core.Core'. Version is unavailable.");
                rawVersionCache = "";
                return rawVersionCache;
            } catch (Exception e) {
                Logger.warn("Failed to retrieve raw version for '{}'.", getId(), e);
                rawVersionCache = "";
                return rawVersionCache;
            }
        }
    }

    /**
     * Returns the fully qualified name of the game entrypoint class.
     *
     * @return entrypoint class name
     */
    @Override
    public String getEntrypoint() {
        return isServer() ? "zombie.network.GameServer" : "zombie.gameStates.MainScreenState";
    }

    /**
     * Returns the execution environment for this installation.
     *
     * @return {@link Environment#SERVER} for server installations, otherwise {@link Environment#CLIENT}
     */
    @Override
    public Environment getEnvironment() {
        return isServer() ? Environment.SERVER : Environment.CLIENT;
    }

    /**
     * Discovers and returns Java libraries required by the game.
     *
     * <p>
     * For server installations, libraries are discovered inside {@code &lt;launchDir&gt;/java}.
     * For client installations, libraries are discovered directly in {@code &lt;launchDir&gt;}.
     * Only files matching {@code *.jar} at depth 1 are included.
     * </p>
     *
     * <p>
     * The result is cached after the first computation. Returned list is immutable.
     * </p>
     *
     * @return immutable list of JAR paths, possibly empty
     */
    @Override
    public List<Path> getJavaLibs() {
        List<Path> cached = javaLibsCache;
        if (cached != null) {
            return cached;
        }

        synchronized (cacheLock) {
            if (javaLibsCache != null) {
                return javaLibsCache;
            }

            Path baseDir;
            try {
                Path launchDir = launchDirectory();
                baseDir = isServer() ? launchDir.resolve("java") : launchDir;
            } catch (Exception e) {
                Logger.warn("Failed to resolve launch directory while discovering Java libs for '{}'.", getId(), e);
                javaLibsCache = List.of();
                return javaLibsCache;
            }

            if (!Files.isDirectory(baseDir)) {
                Logger.debug("Java libs base directory does not exist: '{}'", baseDir);
                javaLibsCache = List.of();
                return javaLibsCache;
            }

            List<Path> jars = new ArrayList<>();
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(baseDir, "*.jar")) {
                for (Path path : stream) {
                    if (Files.isRegularFile(path)) {
                        jars.add(path.toAbsolutePath().normalize());
                    }
                }
            } catch (Exception e) {
                Logger.warn("Failed to list Java libs in '{}' for '{}'.", baseDir, getId(), e);
                javaLibsCache = List.of();
                return javaLibsCache;
            }

            jars.sort(Path::compareTo);
            Logger.debug("Discovered {} Java lib(s) in '{}'", jars.size(), baseDir);

            javaLibsCache = List.copyOf(jars);
            return javaLibsCache;
        }
    }

    /**
     * Discovers and returns native library search directories.
     *
     * <p>
     * The launch directory itself is included when present. For server installations,
     * subdirectories of {@code &lt;launchDir&gt;/natives} are added (depth 1).
     * For client installations, the native directory is determined from OS and architecture
     * (e.g. {@code win64}, {@code linux64}, {@code mac64}).
     * </p>
     *
     * <p>
     * The result is cached after the first computation. Returned list is immutable.
     * </p>
     *
     * @return immutable list of native search paths, possibly empty
     */
    @Override
    public List<Path> getNativePaths() {
        List<Path> cached = nativePathsCache;
        if (cached != null) {
            return cached;
        }

        synchronized (cacheLock) {
            if (nativePathsCache != null) {
                return nativePathsCache;
            }

            Set<Path> result = new LinkedHashSet<>();
            Path launchDir;

            try {
                launchDir = launchDirectory().toAbsolutePath().normalize();
            } catch (Exception e) {
                Logger.warn("Failed to resolve launch directory while discovering native paths for '{}'.", getId(), e);
                nativePathsCache = List.of();
                return nativePathsCache;
            }

            if (Files.isDirectory(launchDir)) {
                result.add(launchDir);
            }

            if (isServer()) {
                Path nativesRoot = launchDir.resolve("natives");
                if (Files.isDirectory(nativesRoot)) {
                    result.add(nativesRoot);

                    try (DirectoryStream<Path> stream = Files.newDirectoryStream(nativesRoot)) {
                        for (Path path : stream) {
                            if (Files.isDirectory(path)) {
                                result.add(path.toAbsolutePath().normalize());
                            }
                        }
                    } catch (Exception e) {
                        Logger.warn("Failed to enumerate server natives in '{}'.", nativesRoot, e);
                    }
                } else {
                    Logger.debug("Server natives directory not found: '{}'", nativesRoot);
                }
            } else {
                Path nativeDir = determineClientNativeDir(launchDir);
                if (nativeDir != null && Files.isDirectory(nativeDir)) {
                    result.add(nativeDir.toAbsolutePath().normalize());
                } else if (nativeDir != null) {
                    Logger.debug("Client native directory not found: '{}'", nativeDir);
                }
            }

            nativePathsCache = List.copyOf(result);
            Logger.debug("Discovered {} native path(s) for '{}'", nativePathsCache.size(), getId());
            return nativePathsCache;
        }
    }

    /**
     * Returns provider metadata.
     *
     * <p>
     * Metadata is built lazily and cached. Values reflect the computed environment and version.
     * </p>
     *
     * @return metadata instance, never {@code null}
     */
    @Override
    public Metadata getMetadata() {
        Metadata cached = metadataCache;
        if (cached != null) {
            return cached;
        }

        synchronized (cacheLock) {
            if (metadataCache != null) {
                return metadataCache;
            }

            metadataCache = new Metadata.Builder()
                    .name(getGameName())
                    .id(getId())
                    .author(getGameAuthor())
                    .environment(getEnvironment().getValue())
                    .version(getNormalizedVersion())
                    .license("Proprietary")
                    .type(PluginType.PROVIDER)
                    .contacts("https://projectzomboid.com")
                    .entryPoints(List.of(getEntrypoint()))
                    .build();

            Logger.debug("Metadata created for '{}'", getId());
            return metadataCache;
        }
    }

    /**
     * Resolves the launch directory for the installation.
     *
     * <p>
     * The directory is derived from the provider code location:
     * the parent directory of the JAR (or classes directory) that contains this class.
     * </p>
     *
     * @return launch directory path
     * @throws IllegalStateException if the location cannot be resolved
     */
    @Override
    public Path launchDirectory() {
        try {
            CodeSource codeSource = getClass().getProtectionDomain().getCodeSource();
            if (codeSource == null) {
                throw new IllegalStateException("CodeSource is null");
            }

            URL location = codeSource.getLocation();
            Path jarOrDir = Paths.get(location.toURI()).toAbsolutePath().normalize();

            Path parent = jarOrDir.getParent();
            if (parent == null) {
                throw new IllegalStateException("Cannot resolve parent directory from: " + jarOrDir);
            }
            return parent;

        } catch (URISyntaxException e) {
            throw new IllegalStateException("Invalid CodeSource URI for provider: " + getId(), e);
        }
    }

    /**
     * Returns system properties to apply during initialization.
     *
     * <p>
     * These properties configure the underlying game runtime. For server installations,
     * {@code java.awt.headless=true} is additionally enabled.
     * </p>
     *
     * @return immutable map of system properties to apply
     */
    @Override
    public Map<String, String> getProviderCoreArgs() {
        // Headless is usually expected for server. Keep it enabled for server only.
        if (isServer()) {
            return Map.of(
                    BASE_CORE_ARGS.keySet().toArray(new String[0])[0], BASE_CORE_ARGS.values().toArray(new String[0])[0],
                    BASE_CORE_ARGS.keySet().toArray(new String[0])[1], BASE_CORE_ARGS.values().toArray(new String[0])[1],
                    "java.awt.headless", "true"
            );
        }
        return BASE_CORE_ARGS;
    }

    /**
     * Redirects {@link System#out} and {@link System#err} to TinyLog using a line-based parser.
     *
     * <p>
     * This method is idempotent: repeated calls after successful redirection are ignored.
     * Output is converted to UTF-8 and each line is parsed by {@link ZomboidLogLineParser}
     * before being forwarded to {@link Logger}.
     * </p>
     *
     * @throws IllegalStateException if redirection fails
     */
    @Override
    public void redirectSystemStreamsToLogger() {
        if (streamsRedirected) {
            Logger.debug("System streams already redirected for '{}'. Skipping.", getId());
            return;
        }

        synchronized (initLock) {
            try {
                System.setOut(new PrintStream(
                        new LineReadingOutputStream(new ZomboidLogLineParser(Logger::info)),
                        true,
                        StandardCharsets.UTF_8
                ));
                System.setErr(new PrintStream(
                        new LineReadingOutputStream(new ZomboidLogLineParser(Logger::error)),
                        true,
                        StandardCharsets.UTF_8
                ));
                streamsRedirected = true;
                Logger.info("System streams redirected to logger for '{}'.", getId());
            } catch (Exception e) {
                Logger.error("Failed to redirect system streams for '{}'.", getId(), e);
                throw new IllegalStateException("Failed to redirect system streams for: " + getId(), e);
            }
        }
    }

    /**
     * Determines whether this installation should be treated as a dedicated server.
     *
     * <p>
     * The detection is performed once and cached. The current heuristic checks whether
     * {@code &lt;launchDir&gt;/java} exists and is a directory.
     * </p>
     *
     * @return {@code true} if this is a server installation, otherwise {@code false}
     */
    public boolean isServer() {
        Boolean cached = isServerInstallCache;
        if (cached != null) {
            return cached;
        }

        synchronized (cacheLock) {
            if (isServerInstallCache != null) {
                return isServerInstallCache;
            }

            try {
                Path launchDir = launchDirectory();
                isServerInstallCache = Files.isDirectory(launchDir.resolve("java"));
            } catch (Exception e) {
                Logger.warn("Failed to detect server install mode for '{}'. Defaulting to CLIENT.", getId(), e);
                isServerInstallCache = false;
            }
            return isServerInstallCache;
        }
    }

    /**
     * Determines the client native library directory based on OS name and architecture.
     *
     * <p>
     * The directory name is derived as follows:
     * <ul>
     *   <li>Windows: {@code win32} or {@code win64}</li>
     *   <li>Linux: {@code linux32} or {@code linux64}</li>
     *   <li>macOS: {@code mac64}</li>
     * </ul>
     * If the OS cannot be classified, {@code null} is returned.
     * </p>
     *
     * @param launchDir normalized launch directory
     * @return candidate native directory path, or {@code null} if OS is unknown
     */
    private static Path determineClientNativeDir(Path launchDir) {
        String os = Optional.ofNullable(System.getProperty("os.name")).orElse("").toLowerCase();
        String arch = Optional.ofNullable(System.getProperty("os.arch")).orElse("").toLowerCase();
        boolean is64Bit = arch.contains("64") || arch.contains("aarch64");

        if (os.contains("win")) {
            return launchDir.resolve(is64Bit ? "win64" : "win32");
        }
        if (os.contains("linux")) {
            return launchDir.resolve(is64Bit ? "linux64" : "linux32");
        }
        if (os.contains("mac")) {
            return launchDir.resolve("mac64");
        }

        Logger.debug("Unknown OS '{}'. Client native path cannot be determined.", os);
        return null;
    }

    /**
     * Getting Application Launch Arguments
     *
     * @return application launch arguments passed from the Main entry point
     */
    @Override
    public String[] getLaunchArgs() {
        return launchArgs;
    }
}