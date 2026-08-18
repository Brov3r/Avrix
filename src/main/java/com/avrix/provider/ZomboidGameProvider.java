package com.avrix.provider;

import com.avrix.core.Environment;
import com.avrix.core.KnotClassLoader;
import com.avrix.core.ServiceManager;
import com.avrix.logger.LineReadingOutputStream;
import com.avrix.logger.ZomboidLogLineParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.classfile.*;
import java.lang.classfile.constantpool.PoolEntry;
import java.lang.classfile.constantpool.Utf8Entry;
import java.lang.classfile.instruction.ConstantInstruction;
import java.lang.classfile.instruction.InvokeInstruction;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
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
 * Production-ready Game Provider implementation for Project Zomboid (v42).
 * <p>
 * Manages the entire execution lifecycle of the game process, including bytecode-level version
 * extraction via the Java 25 ClassFile API, automatic native library topology discovery (Windows,
 * Linux, macOS), flat classpath construction, system stream redirection, and high-performance
 * launch execution via {@link MethodHandle}.
 *
 * @apiNote Must be initialized via {@link #init()} prior to calling {@link #launch(String[])}.
 */
public class ZomboidGameProvider implements GameProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZomboidGameProvider.class);

    private static final String DEFAULT_PROVIDER_ID = "project-zomboid";
    private static final String CLIENT_ENTRYPOINT = "zombie.gameStates.MainScreenState";
    private static final String SERVER_ENTRYPOINT = "zombie.network.GameServer";

    private static final Map<String, String> BASE_CORE_PROPERTIES = Map.of(
            "zomboid.steam", "1",
            "zomboid.znetlog", "1"
    );

    private static final Pattern NORMALIZED_VERSION_PATTERN =
            Pattern.compile("^(\\d+)\\.(\\d+)(?:\\+[^.]*)?\\.(\\d+)");

    private KnotClassLoader classLoader;
    private String[] launchArgs = new String[0];
    private String rawVersion;
    private String normalizedVersion;

    private volatile Boolean serverEnvironmentDetected;
    private volatile boolean initialized = false;
    private volatile boolean streamsRedirected = false;

    /**
     * Initializes the game provider environment, synchronizes process working directory,
     * applies mandatory system properties, and populates the {@link KnotClassLoader} with
     * native directories and bundled library JARs.
     *
     * @throws IllegalStateException if {@link KnotClassLoader} is not registered in {@link ServiceManager}
     * @apiNote Ensures working directory ({@code user.dir}) matches the game root to resolve media resources.
     */
    @Override
    public synchronized void init() {
        if (initialized) {
            LOGGER.debug("{} provider already initialized. Skipping.", getName());
            return;
        }

        LOGGER.info("Initializing game provider [{}]...", getId());

        this.classLoader = ServiceManager.find(KnotClassLoader.class)
                .orElseThrow(() -> new IllegalStateException("KnotClassLoader is not registered in ServiceManager"));

        // Synchronize working directory with game root directory
        Path launchDirectory = getLaunchDirectory();
        System.setProperty("user.dir", launchDirectory.toAbsolutePath().toString());

        Map<String, String> providerProperties = getProviderArgs();
        if (!providerProperties.isEmpty()) {
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
            System.setProperty("org.lwjgl.librarypath", combinedNativePath);
            System.setProperty("java.library.path", combinedNativePath);

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
    public synchronized void launch(String[] args) {
        if (!initialized) {
            throw new IllegalStateException("Provider must be initialized before launch. Call init() first.");
        }

        this.launchArgs = (args == null) ? new String[0] : args.clone();

        Thread currentThread = Thread.currentThread();
        ClassLoader previousContextLoader = currentThread.getContextClassLoader();

        try {
            currentThread.setContextClassLoader(classLoader);

            String entrypointClassName = getEntrypoint();
            LOGGER.info("Launching {} (env={}, version={}) via [{}]",
                    getName(), getEnvironment(), getNormalizedVersion(), entrypointClassName);

            Class<?> entryClass = Class.forName(entrypointClassName, true, classLoader);
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            MethodHandle mainMethodHandle = lookup.findStatic(
                    entryClass,
                    "main",
                    MethodType.methodType(void.class, String[].class)
            );

            mainMethodHandle.invokeExact(launchArgs);

        } catch (NoSuchMethodException e) {
            LOGGER.error("Entrypoint [{}] does not declare public static void main(String[])", getEntrypoint(), e);
            throw new IllegalStateException("Entrypoint missing main method: " + getEntrypoint(), e);
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
        return DEFAULT_PROVIDER_ID;
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

        String raw = getRawVersion();
        Matcher matcher = NORMALIZED_VERSION_PATTERN.matcher(raw);
        if (matcher.find()) {
            normalizedVersion = matcher.group(1) + "." + matcher.group(2) + "." + matcher.group(3);
            return normalizedVersion;
        }

        LOGGER.warn("Unable to parse semantic version from raw string '{}'. Fallback to '0.0.0'", raw);
        normalizedVersion = "0.0.0";
        return normalizedVersion;
    }

    /**
     * Inspects game class bytecode via Java 25 ClassFile API to extract accurate raw version tokens.
     *
     * @return formatted raw version string containing major, minor, patch, and revision components
     * @implNote Parses {@code zombie.core.Core} static initializer and {@code zombie.GitVersion} constant pool.
     */
    @Override
    public synchronized String getRawVersion() {
        if (rawVersion != null && !rawVersion.isBlank()) {
            return rawVersion;
        }

        ClassLoader targetLoader = (classLoader != null)
                ? classLoader
                : ServiceManager.find(KnotClassLoader.class).orElse((KnotClassLoader) getClass().getClassLoader());

        int major = 0;
        int minor = 0;
        String extra = "";
        String patch = "0";
        String revision = "";

        // Inspect zombie.core.Core bytecode
        try (InputStream stream = targetLoader.getResourceAsStream("zombie/core/Core.class")) {
            if (stream != null) {
                ClassModel classModel = ClassFile.of().parse(stream.readAllBytes());

                for (MethodModel method : classModel.methods()) {
                    if ("<clinit>".equals(method.methodName().stringValue())) {
                        Optional<CodeModel> codeOptional = method.code();
                        if (codeOptional.isPresent()) {
                            List<Object> constantStack = new ArrayList<>();
                            for (CodeElement element : codeOptional.get()) {
                                if (element instanceof ConstantInstruction ci) {
                                    constantStack.add(ci.constantValue());
                                } else if (element instanceof InvokeInstruction inv) {
                                    if ("<init>".equals(inv.name().stringValue())
                                            && "zombie/core/GameVersion".equals(inv.owner().asInternalName())
                                            && constantStack.size() >= 3) {
                                        int size = constantStack.size();
                                        if (constantStack.get(size - 3) instanceof Integer maj
                                                && constantStack.get(size - 2) instanceof Integer min
                                                && constantStack.get(size - 1) instanceof String ext) {
                                            major = maj;
                                            minor = min;
                                            extra = ext;
                                        }
                                    }
                                }
                            }
                        }
                        break;
                    }
                }

                // Scan constant pool for build patch number
                Pattern patchPattern = Pattern.compile("^\\.([0-9]{1,4})$");
                for (PoolEntry poolEntry : classModel.constantPool()) {
                    if (poolEntry instanceof Utf8Entry utf8Entry) {
                        String stringValue = utf8Entry.stringValue().trim();
                        Matcher matcher = patchPattern.matcher(stringValue);
                        if (matcher.find()) {
                            patch = matcher.group(1);
                            break;
                        }
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.error("Failed to parse bytecode of zombie/core/Core.class", e);
        }

        // Inspect zombie.GitVersion for revision hash
        try (InputStream gitStream = targetLoader.getResourceAsStream("zombie/GitVersion.class")) {
            if (gitStream != null) {
                ClassModel gitModel = ClassFile.of().parse(gitStream.readAllBytes());
                for (PoolEntry poolEntry : gitModel.constantPool()) {
                    if (poolEntry instanceof Utf8Entry utf) {
                        String value = utf.stringValue().trim();
                        if (value.length() >= 7 && value.length() <= 40 && value.matches("^[0-9a-fA-F]+$")) {
                            revision = value;
                            break;
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.debug("GitVersion metadata unavailable in current installation", e);
        }

        String extraSegment = extra.isBlank() ? "" : "+" + extra;
        String revisionSegment = revision.isBlank() ? "" : " " + revision;

        this.rawVersion = "%d.%d%s.%s%s".formatted(major, minor, extraSegment, patch, revisionSegment);
        return rawVersion;
    }

    /**
     * Resolves the root filesystem directory hosting the Project Zomboid installation.
     *
     * @return absolute, normalized {@link Path} of the game installation directory
     * @throws IllegalStateException if resolution fails and no fallback directory can be determined
     * @apiNote Supports overriding via {@code -Davrix.game.dir} or environment variable {@code AVRIX_GAME_DIR}.
     */
    @Override
    public Path getLaunchDirectory() {
        String explicitDir = System.getProperty("avrix.game.dir", System.getProperty("zomboid.dir"));
        if (explicitDir == null || explicitDir.isBlank()) {
            explicitDir = Optional.ofNullable(System.getenv("AVRIX_GAME_DIR"))
                    .orElseGet(() -> System.getenv("ZOMBOID_DIR"));
        }

        if (explicitDir != null && !explicitDir.isBlank()) {
            Path customPath = Paths.get(explicitDir).toAbsolutePath().normalize();
            if (Files.isDirectory(customPath)) {
                return customPath;
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
                return (parent != null) ? parent : path;
            }
            return path;

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

        List<Path> discoveredJars = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(baseDirectory, "*.jar")) {
            for (Path entry : stream) {
                if (Files.isRegularFile(entry)) {
                    discoveredJars.add(entry.toAbsolutePath().normalize());
                }
            }
        } catch (IOException e) {
            LOGGER.warn("Failed to enumerate Java library JARs in [{}]", baseDirectory, e);
            return List.of();
        }

        Collections.sort(discoveredJars);
        LOGGER.debug("Discovered {} Java library JAR(s) in [{}]", discoveredJars.size(), baseDirectory);
        return List.copyOf(discoveredJars);
    }

    /**
     * Discovers and resolves all operating-system-specific native binary directories.
     *
     * @return unmodifiable list of absolute paths containing native libraries
     */
    @Override
    public List<Path> getNativeLibsPath() {
        Path launchDirectory = getLaunchDirectory();
        if (!Files.isDirectory(launchDirectory)) {
            LOGGER.debug("Launch directory is not accessible: [{}]", launchDirectory);
            return List.of();
        }

        Set<Path> nativePaths = new LinkedHashSet<>();
        nativePaths.add(launchDirectory);

        Path commonNatives = launchDirectory.resolve("natives");
        if (Files.isDirectory(commonNatives)) {
            nativePaths.add(commonNatives);
        }

        if (isServer()) {
            collectServerNatives(launchDirectory, nativePaths);
        } else {
            collectClientNatives(launchDirectory, nativePaths);
        }

        LOGGER.debug("Discovered {} native library path(s) for [{}]", nativePaths.size(), getId());
        return List.copyOf(nativePaths);
    }

    /**
     * Traverses and collects native binary folders specific to the Dedicated Server layout.
     *
     * @param launchDirectory the base installation directory
     * @param nativeCollector set accumulator for native folder paths
     */
    private void collectServerNatives(Path launchDirectory, Set<Path> nativeCollector) {
        Path serverNativesRoot = launchDirectory.resolve("natives");
        if (!Files.isDirectory(serverNativesRoot)) {
            LOGGER.debug("Dedicated server 'natives' directory not located at [{}]", serverNativesRoot);
            return;
        }

        nativeCollector.add(serverNativesRoot);
        try (var stream = Files.list(serverNativesRoot)) {
            stream.filter(Files::isDirectory)
                    .map(Path::toAbsolutePath)
                    .map(Path::normalize)
                    .forEach(nativeCollector::add);
        } catch (IOException e) {
            LOGGER.warn("Failed to traverse server native directories in [{}]", serverNativesRoot, e);
        }
    }

    /**
     * Inspects and collects native binary folders specific to the graphical Client layout.
     *
     * @param launchDirectory the base installation directory
     * @param nativeCollector set accumulator for native folder paths
     */
    private void collectClientNatives(Path launchDirectory, Set<Path> nativeCollector) {
        List<Path> potentialPaths = resolveClientNativeCandidates(launchDirectory);
        for (Path path : potentialPaths) {
            if (Files.isDirectory(path)) {
                nativeCollector.add(path.toAbsolutePath().normalize());
            }
        }
    }

    /**
     * Generates OS-specific directory candidates for graphical client native libraries.
     *
     * @param launchDirectory the root game directory
     * @return ordered list of candidate native directory paths
     */
    private static List<Path> resolveClientNativeCandidates(Path launchDirectory) {
        String os = Optional.ofNullable(System.getProperty("os.name")).orElse("").toLowerCase(Locale.ROOT);
        String arch = Optional.ofNullable(System.getProperty("os.arch")).orElse("").toLowerCase(Locale.ROOT);
        boolean is64Bit = arch.contains("64") || arch.contains("aarch64");

        List<Path> candidates = new ArrayList<>();

        if (os.contains("win")) {
            candidates.add(launchDirectory.resolve(is64Bit ? "win64" : "win32"));
            candidates.add(launchDirectory.resolve("natives").resolve(is64Bit ? "win64" : "win32"));
            candidates.add(launchDirectory.resolve("natives").resolve(is64Bit ? "windows-x86_64" : "windows-x86"));
        } else if (os.contains("linux")) {
            candidates.add(launchDirectory.resolve(is64Bit ? "linux64" : "linux32"));
            candidates.add(launchDirectory.resolve("natives").resolve(is64Bit ? "linux64" : "linux32"));
            candidates.add(launchDirectory.resolve("natives").resolve(is64Bit ? "linux-x86_64" : "linux-x86"));
        } else if (os.contains("mac")) {
            candidates.add(launchDirectory.resolve("mac64"));
            candidates.add(launchDirectory.resolve("natives").resolve("mac64"));
            candidates.add(launchDirectory.resolve("natives").resolve("osx"));
        }

        return candidates;
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

        try {
            System.setOut(new PrintStream(
                    new LineReadingOutputStream(new ZomboidLogLineParser(
                            LOGGER::error,
                            LOGGER::warn,
                            LOGGER::info,
                            LOGGER::debug,
                            LOGGER::trace,
                            LOGGER::info
                    ), StandardCharsets.UTF_8),
                    true,
                    StandardCharsets.UTF_8
            ));

            System.setErr(new PrintStream(
                    new LineReadingOutputStream(new ZomboidLogLineParser(
                            LOGGER::error,
                            LOGGER::warn,
                            LOGGER::error,
                            LOGGER::debug,
                            LOGGER::trace,
                            LOGGER::error
                    ), StandardCharsets.UTF_8),
                    true,
                    StandardCharsets.UTF_8
            ));

            this.streamsRedirected = true;
        } catch (Exception e) {
            LOGGER.error("Failed to redirect system IO streams", e);
            throw new IllegalStateException("Failed to configure stream redirection for: " + getId(), e);
        }
    }

    /**
     * Determines whether the current installation is running in Dedicated Server topology.
     *
     * @return {@code true} if running as dedicated server, {@code false} otherwise
     */
    public synchronized boolean isServer() {
        if (serverEnvironmentDetected != null) {
            return serverEnvironmentDetected;
        }

        try {
            this.serverEnvironmentDetected = Files.isDirectory(getLaunchDirectory().resolve("java"));
        } catch (Exception e) {
            LOGGER.warn("Failed to deduce server environment topology. Defaulting to CLIENT.", e);
            this.serverEnvironmentDetected = false;
        }

        return serverEnvironmentDetected;
    }
}