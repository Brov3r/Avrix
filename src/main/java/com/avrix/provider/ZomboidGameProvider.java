package com.avrix.provider;

import com.avrix.enums.Environment;
import com.avrix.loaders.CoreClassLoader;
import com.avrix.logging.LineReadingOutputStream;
import com.avrix.logging.ZomboidLogLineParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

public class ZomboidGameProvider implements GameProvider {
    private static final Logger log = LoggerFactory.getLogger(ZomboidGameProvider.class);

    private static final Map<String, String> BASE_CORE_ARGS = Map.of(
            "zomboid.steam", "1",
            "zomboid.znetlog", "1"
    );

    private static final Pattern VERSION_PATTERN = Pattern.compile("(\\d+\\.\\d+\\.\\d+)");
    private static final String UNKNOWN_VERSION = "??.??.??";

    private String rawVersionCache;
    private String normalizedVersionCache;
    private CoreClassLoader classLoader;

    private String[] launchArgs;

    private volatile Class<?> gameClass;

    private boolean streamRedirected = false;
    private boolean serverDetectCache = false;
    private boolean serverDetect = false;
    private boolean initialized = false;

    /**
     * Initializes the provider with the given class loader.
     * <p>
     * This method should set up the necessary class loader and prepare any required resources, such as libraries or
     * native paths.
     * </p>
     *
     * @param classLoader the {@link CoreClassLoader} used for game execution; must not be {@code null}.
     */
    @Override
    public synchronized void initialize(CoreClassLoader classLoader) {
        Objects.requireNonNull(classLoader, "CoreClassLoader must not be null");

        if (initialized) {
            log.debug("{} provider already initialized. Skipping.", getName());
            return;
        }

        log.info("Initializing provider '{}'", getId());

        this.classLoader = classLoader;

        Thread thread = Thread.currentThread();
        ClassLoader previous = thread.getContextClassLoader();

        try {
            thread.setContextClassLoader(classLoader);

            Map<String, String> props = getProviderCoreArgs();
            if (!props.isEmpty()) {
                props.forEach(System::setProperty);
                log.debug("Applied {} system properties for '{}'", props.size(), getId());
            }

            List<Path> natives = getNativePaths();
            if (!natives.isEmpty()) {
                classLoader.addNativePaths(natives);
                log.debug("Registered {} native search path(s) for '{}'", natives.size(), getId());
            }

            List<Path> libs = getJavaLibs();
            if (!libs.isEmpty()) {
                for (Path jar : libs) {
                    classLoader.addURL(jar.toUri().toURL());
                }
                log.debug("Added {} classpath URL(s) for '{}'", libs.size(), getId());
            }

            String entry = getEntrypoint();
            gameClass = classLoader.loadClass(entry);

            initialized = true;
            log.info("Provider '{}' initialized successfully. Environment={}", getId(), getEnvironment());

        } catch (Exception e) {
            this.gameClass = null;
            this.classLoader = null;
            this.initialized = false;

            log.error("Failed to initialize provider '{}'", getId(), e);
            throw new IllegalStateException("Failed to initialize provider: " + getId(), e);
        } finally {
            thread.setContextClassLoader(previous);
        }
    }

    /**
     * Launches the game by invoking the entry point.
     * <p>
     * This method should be called after the provider has been initialized. It typically launches the game by
     * invoking the `main(String[])` method of the game entry point class.
     * </p>
     *
     * @param args command-line arguments to pass to the game entry point; may be {@code null}.
     */
    @Override
    public synchronized void launch(String[] args) {
        if (!initialized) {
            throw new IllegalStateException("Provider must be initialized before launch. Call initialize() first.");
        }
        if (classLoader == null || gameClass == null) {
            throw new IllegalStateException("Provider is in an invalid state: game classloader or entrypoint class is null.");
        }

        launchArgs = (args == null) ? new String[0] : args;

        Thread thread = Thread.currentThread();
        ClassLoader previous = thread.getContextClassLoader();

        try {
            thread.setContextClassLoader(classLoader);

            log.info("Launching {} (env={}, version={}) via {}",
                    getName(), getEnvironment(), getNormalizedVersion(), getEntrypoint());

            Method main = gameClass.getDeclaredMethod("main", String[].class);
            main.setAccessible(true);
            main.invoke(null, (Object) launchArgs);

        } catch (NoSuchMethodException e) {
            log.error("Entrypoint '{}' does not declare main(String[]).", getEntrypoint(), e);
            throw new IllegalStateException("Entrypoint does not declare main(String[]): " + getEntrypoint(), e);
        } catch (Exception e) {
            log.error("Failed to launch game via provider '{}'", getId(), e);
            throw new IllegalStateException("Failed to launch game via provider: " + getId(), e);
        } finally {
            thread.setContextClassLoader(previous);
        }
    }

    /**
     * Returns the contacts of the game provider.
     *
     * @return the contacts of the game provider.
     */
    @Override
    public String getContacts() {
        return "https://projectzomboid.com";
    }

    /**
     * Returns the launch arguments passed to the game provider.
     * <p>
     * This method can be used to retrieve any arguments provided when launching the game.
     * </p>
     *
     * @return the launch arguments, never {@code null}.
     */
    @Override
    public String[] getLaunchArgs() {
        return launchArgs;
    }

    /**
     * Returns the name of the game provider.
     *
     * @return the name of the game provider.
     */
    @Override
    public String getName() {
        return isServer() ? "Project Zomboid Server" : "Project Zomboid";
    }

    /**
     * Returns the unique identifier for the game provider.
     *
     * @return the identifier of the game provider.
     */
    @Override
    public String getId() {
        return "project-zomboid";
    }

    /**
     * Returns the author of the game provider.
     *
     * @return the author of the game provider.
     */
    @Override
    public String getAuthor() {
        return "The Indie Stone";
    }

    /**
     * Returns the license type of the game provider.
     *
     * @return the license of the game provider.
     */
    @Override
    public String getLicense() {
        return "PROPRIETARY";
    }

    /**
     * Returns the normalized version string of the game.
     * <p>
     * This method returns the version in a standard format (e.g., major.minor.patch). If the version cannot
     * be determined, a default value will be returned.
     * </p>
     *
     * @return the normalized version of the game, never {@code null}.
     */
    @Override
    public synchronized String getNormalizedVersion() {
        if (normalizedVersionCache != null) return normalizedVersionCache;

        String raw = getRawVersion();

        if (raw.isBlank()) {
            log.debug("Raw version is empty; using '{}' for '{}'", UNKNOWN_VERSION, getId());
            return UNKNOWN_VERSION;
        }

        Matcher matcher = VERSION_PATTERN.matcher(raw);
        normalizedVersionCache = matcher.find() ? matcher.group(1) : raw;
        return normalizedVersionCache;
    }

    /**
     * Returns the raw version string of the game.
     * <p>
     * This method returns the version string as retrieved directly from the game runtime. It may include extra
     * metadata or an unprocessed version string.
     * </p>
     *
     * @return the raw version string of the game, or {@code null} if unavailable.
     */
    @Override
    public String getRawVersion() {
        if (rawVersionCache != null) return rawVersionCache;

        if (classLoader == null) {
            throw new IllegalStateException("ClassLoader not initialized. Call initialize() first.");
        }

        try {
            Class<?> coreClass = classLoader.loadClass("zombie.core.Core");

            Method getInstance = coreClass.getDeclaredMethod("getInstance");
            getInstance.setAccessible(true);
            Object coreInstance = getInstance.invoke(null);

            Method getVersion = coreClass.getDeclaredMethod("getVersion");
            getVersion.setAccessible(true);
            Object version = getVersion.invoke(coreInstance);

            rawVersionCache = (version instanceof String s) ? s : "";
            return rawVersionCache;

        } catch (ClassNotFoundException e) {
            log.warn("Cannot load 'zombie.core.Core'. Version is unavailable.");
            rawVersionCache = "";
            return rawVersionCache;
        } catch (Exception e) {
            log.warn("Failed to retrieve raw version for '{}'.", getId(), e);
            rawVersionCache = "";
            return rawVersionCache;
        }
    }

    /**
     * Returns the launch directory for the game.
     * <p>
     * This method returns the directory where the game executable or core files are located.
     * </p>
     *
     * @return the path to the launch directory.
     */
    @Override
    public Path getLaunchDirectory() {
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
     * Returns the entry point class name for the game.
     * <p>
     * This method returns the class name that contains the main method used to launch the game.
     * </p>
     *
     * @return the entry point class name.
     */
    @Override
    public String getEntrypoint() {
        return isServer() ? "zombie.network.GameServer" : "zombie.gameStates.MainScreenState";
    }

    /**
     * Returns the environment type of the game.
     * <p>
     * This method returns whether the game is running in a server or client environment.
     * </p>
     *
     * @return the environment type (either {@link Environment#CLIENT} or {@link Environment#SERVER}).
     */
    @Override
    public Environment getEnvironment() {
        return isServer() ? Environment.SERVER : Environment.CLIENT;
    }

    /**
     * Returns a list of Java library paths required by the game.
     * <p>
     * This method returns a list of JAR file paths that are required to run the game. By default, it returns
     * an empty list.
     * </p>
     *
     * @return a list of paths to Java libraries, possibly empty.
     */
    @Override
    public List<Path> getJavaLibs() {
        Path baseDir;
        try {
            baseDir = isServer() ? getLaunchDirectory().resolve("java") : getLaunchDirectory();
        } catch (Exception e) {
            log.warn("Failed to resolve launch directory while discovering Java libs for '{}'.", getId(), e);
            return List.of();
        }

        if (!Files.isDirectory(baseDir)) {
            log.debug("Java libs base directory does not exist: '{}'", baseDir);
            return List.of();
        }

        List<Path> jars = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(baseDir, "*.jar")) {
            for (Path path : stream) {
                if (Files.isRegularFile(path)) {
                    jars.add(path.toAbsolutePath().normalize());
                }
            }
        } catch (Exception e) {
            log.warn("Failed to list Java libs in '{}' for '{}'.", baseDir, getId(), e);
            return List.of();
        }

        jars.sort(Path::compareTo);
        log.debug("Discovered {} Java lib(s) in '{}'", jars.size(), baseDir);

        return List.copyOf(jars);
    }


    /**
     * Returns a list of native library search paths required by the game.
     * <p>
     * This method returns a list of directories that contain native libraries required by the game. By default,
     * it returns an empty list.
     * </p>
     *
     * @return a list of paths to native library directories, possibly empty.
     */
    @Override
    public List<Path> getNativePaths() {
        Set<Path> result = new LinkedHashSet<>();
        Path launchDir;

        try {
            launchDir = getLaunchDirectory().toAbsolutePath().normalize();
        } catch (Exception e) {
            log.warn("Failed to resolve launch directory while discovering native paths for '{}'.", getId(), e);
            return List.of();
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
                    log.warn("Failed to enumerate server natives in '{}'.", nativesRoot, e);
                }
            } else {
                log.debug("Server natives directory not found: '{}'", nativesRoot);
            }
        } else {
            Path nativeDir = determineClientNativeDir(launchDir);
            if (nativeDir != null && Files.isDirectory(nativeDir)) {
                result.add(nativeDir.toAbsolutePath().normalize());
            } else if (nativeDir != null) {
                log.debug("Client native directory not found: '{}'", nativeDir);
            }
        }

        log.debug("Discovered {} native path(s) for '{}'", result.size(), getId());
        return List.copyOf(result);
    }

    /**
     * Returns the core arguments used by the game provider.
     * <p>
     * This method returns any additional system properties or arguments that are required for the game provider
     * to function correctly. By default, it returns an empty map.
     * </p>
     *
     * @return a map of core arguments, possibly empty.
     */
    @Override
    public Map<String, String> getProviderCoreArgs() {
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
        if (serverDetectCache) return serverDetect;

        try {
            Path launchDir = getLaunchDirectory();
            serverDetect = Files.isDirectory(launchDir.resolve("java"));
            serverDetectCache = true;
        } catch (Exception e) {
            log.warn("Failed to detect server install mode for '{}'. Defaulting to CLIENT.", getId(), e);
            serverDetectCache = false;
        }
        return serverDetect;
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

        log.debug("Unknown OS '{}'. Client native path cannot be determined.", os);
        return null;
    }

    /**
     * Redirects system output streams (stdout and stderr) to a logger.
     * <p>
     * This method can be overridden to redirect the system output streams to a logging system. By default, this
     * method does nothing (no-op).
     * </p>
     */
    @Override
    public synchronized void redirectSystemStreamsToLogger() {
        if (streamRedirected) return;

        try {
            System.setOut(new PrintStream(
                    new LineReadingOutputStream(new ZomboidLogLineParser(org.tinylog.Logger::info)),
                    true,
                    StandardCharsets.UTF_8
            ));
            System.setErr(new PrintStream(
                    new LineReadingOutputStream(new ZomboidLogLineParser(org.tinylog.Logger::error)),
                    true,
                    StandardCharsets.UTF_8
            ));
            log.info("System streams redirected to logger for '{}'.", getId());

            streamRedirected = true;
        } catch (Exception e) {
            log.error("Failed to redirect system streams for '{}'.", getId(), e);
            throw new IllegalStateException("Failed to redirect system streams for: " + getId(), e);
        }
    }
}
