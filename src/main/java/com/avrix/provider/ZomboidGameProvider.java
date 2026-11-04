package com.avrix.provider;

import com.avrix.core.Environment;
import com.avrix.core.KnotClassLoader;
import com.avrix.logger.LineReadingOutputStream;
import com.avrix.logger.ZomboidLogLineParser;
import com.avrix.utils.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.util.*;

/**
 * Production-ready Game Provider implementation for Project Zomboid (v42).
 * <p>
 * Manages the entire execution lifecycle of the game process, including bytecode-level version
 * extraction via the Java 25 ClassFile API, automatic native library topology discovery (Windows,
 * Linux, macOS), flat classpath construction, system stream redirection, and high-performance
 * launch execution via {@link MethodHandle}.
 *
 * <p><b>API Note:</b> Must be initialized via {@link #init()} prior to calling {@link #launch(String[])}.
 */
public class ZomboidGameProvider implements GameProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZomboidGameProvider.class);

    /**
     * Constructs a new Project Zomboid game provider in an uninitialized state.
     */
    public ZomboidGameProvider() {
    }

    private static final String CLIENT_ENTRYPOINT = "zombie.gameStates.MainScreenState";
    private static final String SERVER_ENTRYPOINT = "zombie.network.GameServer";

    private static final Map<String, String> BASE_CORE_PROPERTIES = Map.of(
            "zomboid.steam", "1",
            "zomboid.znetlog", "1"
    );

    private final NativeLibraryLocator nativeLibraryLocator = new NativeLibraryLocator();
    private final ZomboidVersionResolver versionResolver = new ZomboidVersionResolver();
    private final ZomboidEnvironmentDetector environmentDetector = new ZomboidEnvironmentDetector();

    private KnotClassLoader classLoader;
    private String[] launchArgs = new String[0];

    /**
     * Initializes the game environment with the loader classloader.
     *
     * @param classLoader flat classloader used to resolve game classes and attach libraries
     * @throws NullPointerException if {@code classLoader} is null
     */
    @Override
    public synchronized void init(KnotClassLoader classLoader) {
        this.classLoader = Objects.requireNonNull(classLoader, "KnotClassLoader cannot be null");
        init();
    }
    private String rawVersion;
    private String normalizedVersion;
    private GameVersionInfo resolvedVersion;

    private volatile Boolean serverEnvironmentDetected;
    private volatile boolean initialized = false;
    private volatile boolean streamsRedirected = false;

    private PrintStream originalOut;
    private PrintStream originalErr;
    private PrintStream redirectedOut;
    private PrintStream redirectedErr;
    private String originalUserDir;
    private String originalJavaLibraryPath;
    private String originalLwjglLibraryPath;
    private final Map<String, String> originalProviderProperties = new HashMap<>();
    private final Set<String> absentProviderProperties = new HashSet<>();
    private boolean systemStateCaptured = false;

    /**
     * Initializes the game provider environment, synchronizes process working directory,
     * applies mandatory system properties, and populates the {@link KnotClassLoader} with
     * native directories and bundled library JARs.
     * <p><b>API Note:</b> Ensures working directory ({@code user.dir}) matches the game root to resolve media resources.
     *
     * @throws IllegalStateException if no classloader has been provided
     */
    @Override
    public synchronized void init() {
        if (classLoader == null) {
            throw new IllegalStateException("KnotClassLoader must be provided before init()");
        }

        if (initialized) {
            LOGGER.debug("{} provider already initialized. Skipping.", getName());
            return;
        }

        LOGGER.info("Initializing game provider [{}]...", getId());

        Path launchDirectory = getLaunchDirectory();
        validateInstallation(launchDirectory);

        captureSystemState();
        try {
            // Synchronize working directory with game root directory
            System.setProperty("user.dir", launchDirectory.toAbsolutePath().toString());

            Map<String, String> providerProperties = getProviderArgs();
            if (!providerProperties.isEmpty()) {
                captureProviderProperties(providerProperties);
                providerProperties.forEach(System::setProperty);
                LOGGER.debug("Applied {} system properties for [{}]", providerProperties.size(), getId());
            }

            List<Path> nativePaths = getNativeLibsPath();
            if (!nativePaths.isEmpty()) {
                classLoader.addNativePaths(nativePaths);

                String combinedNativePath = nativePaths.stream()
                        .map(Path::toString)
                        .reduce((a, b) -> a + File.pathSeparator + b)
                        .orElse("");
                System.setProperty("org.lwjgl.librarypath", appendPath(originalLwjglLibraryPath, combinedNativePath));
                System.setProperty("java.library.path", appendPath(originalJavaLibraryPath, combinedNativePath));

                LOGGER.debug("Registered {} native search path(s) for [{}]", nativePaths.size(), getId());
            }

            List<Path> javaLibraries = getJavaLibsPath();
            if (!javaLibraries.isEmpty()) {
                for (Path jarPath : javaLibraries) {
                    try {
                        classLoader.addURL(jarPath.toUri().toURL());
                    } catch (Exception e) {
                        LOGGER.warn("Failed to attach library JAR [{}] to classpath", jarPath, e);
                    }
                }
                LOGGER.debug("Appended {} library JAR(s) to classpath for [{}]", javaLibraries.size(), getId());
            }

            this.initialized = true;
        } catch (RuntimeException | Error e) {
            restoreSystemState();
            throw e;
        }

        LOGGER.info("Provider [{}] initialized successfully. (Environment={})", getId(), getEnvironment());
    }

    /**
     * Launches Project Zomboid by resolving and executing the static entrypoint method
     * via {@link MethodHandle} invocations within the context of {@link KnotClassLoader}.
     *
     * @param args command line arguments passed to the game executable
     * @throws IllegalStateException if the provider has not been initialized or if execution fails
     */
    @Override
    public void launch(String[] args) {
        String entrypointClassName;
        MethodHandle mainMethodHandle;
        String[] gameArgs;

        // Keep the provider monitor only for setup. The game loop can run for the whole process lifetime and may
        // call System.exit() from inside main(), so holding this lock during invocation would deadlock the shutdown hook.
        synchronized (this) {
            if (!initialized) {
                throw new IllegalStateException("Provider must be initialized before launch. Call init() first.");
            }

            this.launchArgs = (args == null) ? new String[0] : args.clone();
            gameArgs = this.launchArgs.clone();
            entrypointClassName = getEntrypoint();

            try {
                Class<?> entryClass = Class.forName(entrypointClassName, true, classLoader);
                MethodHandles.Lookup lookup = MethodHandles.lookup();
                mainMethodHandle = lookup.findStatic(
                        entryClass,
                        "main",
                        MethodType.methodType(void.class, String[].class)
                );
            } catch (NoSuchMethodException e) {
                LOGGER.error("Entrypoint [{}] does not declare public static void main(String[])", entrypointClassName, e);
                throw new IllegalStateException("Entrypoint missing main method: " + entrypointClassName, e);
            } catch (Throwable throwable) {
                LOGGER.error("Failed to resolve entrypoint [{}] for provider [{}]", entrypointClassName, getId(), throwable);
                throw new IllegalStateException("Game entrypoint resolution failed: " + getId(), throwable);
            }
        }

        Thread currentThread = Thread.currentThread();
        ClassLoader previousContextLoader = currentThread.getContextClassLoader();

        try {
            currentThread.setContextClassLoader(classLoader);

            LOGGER.info("Launching {} (env={}, version={}) via [{}]",
                    getName(), getEnvironment(), getNormalizedVersion(), entrypointClassName);

            mainMethodHandle.invokeExact(gameArgs);

        } catch (Throwable throwable) {
            LOGGER.error("Project Zomboid execution terminated unexpectedly via provider [{}]", getId(), throwable);
            throw new IllegalStateException("Game execution failed: " + getId(), throwable);
        } finally {
            currentThread.setContextClassLoader(previousContextLoader);
        }
    }

    /**
     * Retrieves a defensive copy of the arguments supplied to the game process upon launch.
     *
     * @return cloned array of launch arguments
     */
    @Override
    public String[] getLaunchArgs() {
        return launchArgs.clone();
    }

    /**
     * Returns the human-readable display name of the game distribution.
     *
     * @return display name indicating Client or Dedicated Server topology
     */
    @Override
    public String getName() {
        return isServer() ? "Project Zomboid Dedicated Server" : "Project Zomboid";
    }

    /**
     * Returns the unique string identifier of this provider.
     *
     * @return provider identifier
     */
    @Override
    public String getId() {
        return Constants.DEFAULT_GAME_PROVIDER_ID;
    }

    /**
     * Returns the author attribution list of the target game.
     *
     * @return unmodifiable list of game developer entity names
     */
    @Override
    public List<String> getAuthors() {
        return List.of("The Indie Stone");
    }

    /**
     * Returns the licensing model descriptor of the target game.
     *
     * @return license identifier string
     */
    @Override
    public String getLicense() {
        return "PROPRIETARY";
    }

    /**
     * Returns official contact and documentation URLs for the target game.
     *
     * @return list of official URL endpoints
     */
    @Override
    public List<String> getContacts() {
        return List.of("https://projectzomboid.com");
    }

    /**
     * Extracts and computes a normalized semantic version string compliant with SemVer standards.
     *
     * @return normalized semantic version string (e.g., {@code "42.20.2"})
     */
    @Override
    public synchronized String getNormalizedVersion() {
        if (normalizedVersion != null && !normalizedVersion.isBlank()) {
            return normalizedVersion;
        }

        getRawVersion();
        normalizedVersion = resolvedVersion != null
                ? resolvedVersion.normalized()
                : versionResolver.resolveNormalizedVersion(rawVersion);
        return normalizedVersion;
    }

    /**
     * Inspects game class bytecode via Java 25 ClassFile API to extract accurate raw version tokens.
     * <p><b>Implementation Note:</b> Reads {@code projectzomboid.jar} directly when possible, so game classes are never
     * loaded or initialized before launch.
     *
     * @return formatted raw version string containing major, minor, patch, and revision components
     */
    @Override
    public synchronized String getRawVersion() {
        if (rawVersion != null && !rawVersion.isBlank()) {
            return rawVersion;
        }

        Path gameArchive = resolveGameArchive();
        if (gameArchive != null) {
            resolvedVersion = versionResolver.resolve(gameArchive);
        } else {
            ClassLoader targetLoader = (classLoader != null)
                    ? classLoader
                    : getClass().getClassLoader();
            resolvedVersion = versionResolver.resolve(targetLoader);
        }

        this.rawVersion = resolvedVersion.raw();
        return rawVersion;
    }

    /**
     * Resolves the game archive used for bytecode-level version extraction.
     *
     * @return path to {@code projectzomboid.jar}, or {@code null} when it cannot be found
     */
    private Path resolveGameArchive() {
        Path launchDirectory = getLaunchDirectory();
        Path serverArchive = launchDirectory.resolve("java").resolve("projectzomboid.jar");
        if (Files.isRegularFile(serverArchive)) {
            return serverArchive;
        }

        Path clientArchive = launchDirectory.resolve("projectzomboid.jar");
        if (Files.isRegularFile(clientArchive)) {
            return clientArchive;
        }

        LOGGER.debug("Project Zomboid archive not found under [{}]", launchDirectory);
        return null;
    }

    /**
     * Resolves the root filesystem directory hosting the Project Zomboid installation.
     * <p><b>API Note:</b> Supports overriding via {@code -Davrix.game.dir} or environment variable {@code AVRIX_GAME_DIR}.
     * macOS application bundles are resolved to the directory containing the game files.
     *
     * @return absolute, normalized {@link Path} of the game installation directory
     * @throws IllegalStateException if resolution fails and no fallback directory can be determined
     */
    @Override
    public Path getLaunchDirectory() {
        String explicitDir = firstNonBlank(
                System.getProperty("avrix.game.dir"),
                System.getProperty("zomboid.dir")
        );
        if (explicitDir == null) {
            explicitDir = firstNonBlank(
                    System.getenv("AVRIX_GAME_DIR"),
                    System.getenv("ZOMBOID_DIR")
            );
        }

        if (explicitDir != null && !explicitDir.isBlank()) {
            Path customPath = Paths.get(explicitDir).toAbsolutePath().normalize();
            if (Files.isDirectory(customPath)) {
                return ZomboidEnvironmentDetector.resolveMacBundleGameRoot(customPath);
            }
            LOGGER.warn("Explicitly defined game directory does not exist: [{}]", customPath);
        }

        try {
            CodeSource codeSource = getClass().getProtectionDomain().getCodeSource();
            if (codeSource == null) {
                return Paths.get(".").toAbsolutePath().normalize();
            }

            URL location = codeSource.getLocation();
            Path path = Paths.get(location.toURI()).toAbsolutePath().normalize();

            if (Files.isRegularFile(path)) {
                Path parent = path.getParent();
                return ZomboidEnvironmentDetector.resolveMacBundleGameRoot((parent != null) ? parent : path);
            }
            return ZomboidEnvironmentDetector.resolveMacBundleGameRoot(path);

        } catch (URISyntaxException e) {
            throw new IllegalStateException("Invalid URI syntax for provider CodeSource", e);
        }
    }

    /**
     * Returns the binary FQCN of the main entrypoint class for the active execution environment.
     *
     * @return fully qualified binary name of the game entrypoint class
     */
    @Override
    public String getEntrypoint() {
        return isServer() ? SERVER_ENTRYPOINT : CLIENT_ENTRYPOINT;
    }

    /**
     * Resolves the execution environment ({@link Environment#SERVER} or {@link Environment#CLIENT}).
     *
     * @return active runtime environment classification
     */
    @Override
    public Environment getEnvironment() {
        return isServer() ? Environment.SERVER : Environment.CLIENT;
    }

    /**
     * Discovers and enumerates all Java library JARs required by the game classpath.
     *
     * @return sorted, unmodifiable list of absolute paths pointing to library JAR files
     */
    @Override
    public List<Path> getJavaLibsPath() {
        Path baseDirectory = isServer() ? getLaunchDirectory().resolve("java") : getLaunchDirectory();

        if (!Files.isDirectory(baseDirectory)) {
            LOGGER.debug("Java libraries directory does not exist: [{}]", baseDirectory);
            return List.of();
        }

        Path loaderArchive = currentCodeSourcePath();
        List<Path> discoveredJars = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(baseDirectory, "*.jar")) {
            for (Path entry : stream) {
                if (Files.isRegularFile(entry) && !isSamePath(entry, loaderArchive)) {
                    discoveredJars.add(entry.toAbsolutePath().normalize());
                }
            }
        } catch (IOException e) {
            LOGGER.warn("Failed to enumerate Java library JARs in [{}]", baseDirectory, e);
            return List.of();
        }

        discoveredJars.sort(Comparator.comparing(path -> path.getFileName().toString().toLowerCase(Locale.ROOT)));
        LOGGER.debug("Discovered {} Java library JAR(s) in [{}]", discoveredJars.size(), baseDirectory);
        return List.copyOf(discoveredJars);
    }

    /**
     * Resolves the archive or directory containing the provider implementation.
     *
     * @return normalized code-source path, or {@code null} when unavailable
     */
    private Path currentCodeSourcePath() {
        try {
            CodeSource codeSource = getClass().getProtectionDomain().getCodeSource();
            if (codeSource == null || codeSource.getLocation() == null) {
                return null;
            }
            return Paths.get(codeSource.getLocation().toURI()).toAbsolutePath().normalize();
        } catch (URISyntaxException _) {
            return null;
        }
    }

    private static boolean isSamePath(Path first, Path second) {
        if (second == null) {
            return false;
        }
        try {
            return Files.isSameFile(first, second);
        } catch (IOException _) {
            return first.toAbsolutePath().normalize().equals(second);
        }
    }

    /**
     * Discovers and resolves all operating-system-specific native binary directories.
     *
     * @return unmodifiable list of absolute paths containing native libraries
     */
    @Override
    public List<Path> getNativeLibsPath() {
        List<Path> nativePaths = nativeLibraryLocator.locate(getLaunchDirectory(), isServer());
        LOGGER.debug("Discovered {} native library path(s) for [{}]", nativePaths.size(), getId());
        return nativePaths;
    }

    /**
     * Returns the map of essential JVM system properties required to run Project Zomboid.
     *
     * @return unmodifiable map of key-value system property pairs
     */
    @Override
    public Map<String, String> getProviderArgs() {
        if (isServer()) {
            Map<String, String> serverProperties = new HashMap<>(BASE_CORE_PROPERTIES);
            serverProperties.put("java.awt.headless", "true");
            return Collections.unmodifiableMap(serverProperties);
        }
        return BASE_CORE_PROPERTIES;
    }

    /**
     * Indicates that Project Zomboid Lua code relies on the runtime marker to detect an active Avrix launch.
     *
     * @return {@code true} always
     */
    @Override
    public boolean requiresRuntimeMarker() {
        return true;
    }

    /**
     * Intercepts and redirects {@link System#out} and {@link System#err} to the Avrix logging system.
     *
     * @throws IllegalStateException if stream redirection configuration fails
     */
    @Override
    public synchronized void redirectSystemStreamsToLogger() {
        if (streamsRedirected) {
            return;
        }

        LOGGER.info("Redirecting standard output and error streams to Avrix logging subsystem...");

        PrintStream previousOut = System.out;
        PrintStream previousErr = System.err;
        Charset charset = consoleCharset();
        PrintStream newOut = null;
        PrintStream newErr = null;
        try {
            newOut = new PrintStream(
                    new LineReadingOutputStream(new ZomboidLogLineParser(
                            LOGGER::error,
                            LOGGER::warn,
                            LOGGER::info,
                            LOGGER::debug,
                            LOGGER::trace,
                            LOGGER::info
                    ), charset),
                    true,
                    charset
            );

            newErr = new PrintStream(
                    new LineReadingOutputStream(new ZomboidLogLineParser(
                            LOGGER::error,
                            LOGGER::warn,
                            LOGGER::error,
                            LOGGER::debug,
                            LOGGER::trace,
                            LOGGER::error
                    ), charset),
                    true,
                    charset
            );

            System.setOut(newOut);
            System.setErr(newErr);
            this.originalOut = previousOut;
            this.originalErr = previousErr;
            this.redirectedOut = newOut;
            this.redirectedErr = newErr;
            this.streamsRedirected = true;
        } catch (Exception e) {
            closeQuietly(newOut);
            closeQuietly(newErr);
            System.setOut(previousOut);
            System.setErr(previousErr);
            LOGGER.error("Failed to redirect system IO streams", e);
            throw new IllegalStateException("Failed to configure stream redirection for: " + getId(), e);
        }
    }

    /**
     * Restores process-wide state changed by the provider.
     */
    @Override
    public synchronized void close() {
        restoreSystemState();
        initialized = false;
        serverEnvironmentDetected = null;
        rawVersion = null;
        normalizedVersion = null;
        resolvedVersion = null;
        launchArgs = new String[0];
    }

    private void restoreSystemState() {
        try {
            restoreSystemStreams();
        } catch (RuntimeException e) {
            LOGGER.warn("Failed to restore standard streams", e);
        }
        if (!systemStateCaptured) {
            return;
        }

        restoreProviderProperties();
        restoreSystemProperty("user.dir", originalUserDir);
        restoreSystemProperty("java.library.path", originalJavaLibraryPath);
        restoreSystemProperty("org.lwjgl.librarypath", originalLwjglLibraryPath);

        originalUserDir = null;
        originalJavaLibraryPath = null;
        originalLwjglLibraryPath = null;
        systemStateCaptured = false;
    }

    private void captureSystemState() {
        if (systemStateCaptured) {
            return;
        }
        originalUserDir = System.getProperty("user.dir");
        originalJavaLibraryPath = System.getProperty("java.library.path");
        originalLwjglLibraryPath = System.getProperty("org.lwjgl.librarypath");
        systemStateCaptured = true;
    }

    private void captureProviderProperties(Map<String, String> properties) {
        for (String key : properties.keySet()) {
            String value = System.getProperty(key);
            if (value == null) {
                absentProviderProperties.add(key);
            } else {
                originalProviderProperties.put(key, value);
            }
        }
    }

    private void restoreProviderProperties() {
        for (String key : absentProviderProperties) {
            System.clearProperty(key);
        }
        originalProviderProperties.forEach(System::setProperty);
        absentProviderProperties.clear();
        originalProviderProperties.clear();
    }

    private void restoreSystemStreams() {
        if (!streamsRedirected) {
            return;
        }

        flushAndClose(redirectedOut);
        flushAndClose(redirectedErr);
        redirectedOut = null;
        redirectedErr = null;

        if (originalOut != null) {
            System.setOut(originalOut);
            originalOut.flush();
        }
        if (originalErr != null) {
            System.setErr(originalErr);
            originalErr.flush();
        }
        originalOut = null;
        originalErr = null;
        streamsRedirected = false;
    }

    private static Charset consoleCharset() {
        if (System.console() != null) {
            return System.console().charset();
        }
        return System.out.charset();
    }

    private static void flushAndClose(PrintStream stream) {
        if (stream == null) {
            return;
        }
        stream.flush();
        stream.close();
    }

    private static void closeQuietly(PrintStream stream) {
        if (stream == null) {
            return;
        }
        try {
            stream.close();
        } catch (RuntimeException _) {
            // Best-effort cleanup after a failed redirection.
        }
    }

    private static String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return (second != null && !second.isBlank()) ? second : null;
    }

    private static String appendPath(String existing, String addition) {
        if (existing == null || existing.isBlank()) {
            return addition;
        }
        return addition + File.pathSeparator + existing;
    }

    private static void restoreSystemProperty(String key, String value) {
        if (value == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, value);
        }
    }

    /**
     * Determines whether the current installation is running in Dedicated Server topology.
     * <p>
     * Detection prefers explicit environment overrides and the launch configuration, then falls back to launch
     * scripts, marker files, installation layout, Steam metadata, and the directory name.
     *
     * @return {@code true} if running as dedicated server, {@code false} otherwise
     */
    public synchronized boolean isServer() {
        if (serverEnvironmentDetected != null) {
            return serverEnvironmentDetected;
        }

        try {
            Environment environment = environmentDetector.detect(getLaunchDirectory());
            this.serverEnvironmentDetected = environment == Environment.SERVER;
        } catch (Exception e) {
            LOGGER.warn("Failed to deduce server environment topology. Defaulting to CLIENT.", e);
            this.serverEnvironmentDetected = false;
        }

        return serverEnvironmentDetected;
    }

    /**
     * Validates that a directory looks like a Project Zomboid installation.
     *
     * @param launchDirectory candidate installation directory
     * @throws IllegalStateException if the directory does not contain recognizable game files
     */
    private void validateInstallation(Path launchDirectory) {
        if (!Files.isDirectory(launchDirectory)) {
            throw new IllegalStateException(
                    "Project Zomboid installation directory does not exist: " + launchDirectory
                            + ". Set -Davrix.game.dir or AVRIX_GAME_DIR."
            );
        }

        boolean hasGameArchive = Files.isRegularFile(launchDirectory.resolve("projectzomboid.jar"))
                || Files.isRegularFile(launchDirectory.resolve("java").resolve("projectzomboid.jar"));
        boolean hasClientLayout = ZomboidEnvironmentDetector.looksLikeClientLayout(launchDirectory)
                || environmentDetector.hasClientMarker(launchDirectory)
                || Files.isDirectory(launchDirectory.resolve("jre64"));
        boolean hasServerLayout = ZomboidEnvironmentDetector.looksLikeServerLayout(launchDirectory)
                || environmentDetector.hasServerMarker(launchDirectory);

        if (!hasGameArchive && !hasClientLayout && !hasServerLayout) {
            throw new IllegalStateException(
                    "Directory does not look like a Project Zomboid installation: " + launchDirectory
                            + ". Set -Davrix.game.dir or AVRIX_GAME_DIR to the game root."
            );
        }
    }
}