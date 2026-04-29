package com.avrix.provider;

import com.avrix.core.BaseClassLoader;
import com.avrix.core.Environment;
import com.avrix.logger.LineReadingOutputStream;
import com.avrix.logger.ZomboidLogLineParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.lang.classfile.*;
import java.lang.classfile.instruction.*;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
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
 * Provider for loading the Project Zomboid client/server application
 */
public class ZomboidGameProvider implements GameProvider {
    private static final Logger log = LoggerFactory.getLogger(ZomboidGameProvider.class);

    private static final Map<String, String> BASE_CORE_ARGS = Map.of(
            "zomboid.steam", "1",
            "zomboid.znetlog", "1"
    );

    private URLClassLoader classLoader;
    private volatile Class<?> gameClass;

    private String[] launchArgs;
    private String rawVersion;
    private String normalizedVersion;

    private Boolean isServerDetect;
    private Boolean isInitialized;
    private Boolean isStreamRedirected;


    /**
     * Prepares the provider with an isolated classloader.
     * Invoked once before {@link #launch(String[])}.
     *
     * @param classLoader the loader for game and extension classes
     */
    @Override
    public void initialize(BaseClassLoader classLoader) {
        Objects.requireNonNull(classLoader, "ClassLoader must not be null");

        if (isInitialized != null) {
            log.debug("{} provider already initialized. Skipping.", getName());
            return;
        }

        log.info("Initializing provider '{}'", getId());

        Map<String, String> props = getProviderArgs();
        if (!props.isEmpty()) {
            props.forEach(System::setProperty);
            log.debug("Applied {} system properties for '{}'", props.size(), getId());
        }

        List<Path> natives = getNativeLibsPath();
        if (!natives.isEmpty()) {
            classLoader.addNativePaths(natives);
            log.debug("Registered {} native search path(s) for '{}'", natives.size(), getId());
        }

        List<Path> libs = getJavaLibsPath();
        if (!libs.isEmpty()) {
            for (Path jar : libs) {
                try {
                    classLoader.addURL(jar.toUri().toURL());
                } catch (Exception e) {
                    log.warn("Failed to add JAR to classloader: '{}'", jar, e);
                }
            }
            log.debug("Added {} classpath URL(s) for '{}'", libs.size(), getId());
        }

        isInitialized = true;
        this.classLoader = classLoader;

        log.info("Provider '{}' initialized successfully. (Environment={})", getId(), getEnvironment());
    }

    /**
     * Starts the game or runtime with the final merged arguments.
     *
     * @param args resolved command-line arguments including provider defaults and user overrides
     */
    @Override
    public synchronized void launch(String[] args) {
        if (isInitialized == null) {
            throw new IllegalStateException("Provider must be initialized before launch. Call initialize() first.");
        }
        if (classLoader == null) {
            throw new IllegalStateException("Provider is in an invalid state: game classloader is null.");
        }

        launchArgs = (args == null) ? new String[0] : args;

        Thread thread = Thread.currentThread();
        ClassLoader previous = thread.getContextClassLoader();

        try {
            thread.setContextClassLoader(classLoader);

            gameClass = classLoader.loadClass(getEntrypoint());

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
     * Provides the default command-line arguments required by this provider.
     *
     * @return an array of default arguments, may be empty
     */
    @Override
    public String[] getLaunchArgs() {
        return launchArgs;
    }

    /**
     * Returns the human-readable display name of the game or provider.
     *
     * @return the display name
     */
    @Override
    public String getName() {
        return isServer() ? "Project Zomboid Server" : "Project Zomboid";
    }

    /**
     * Returns the unique human-readable identifier used for internal routing.
     *
     * @return the identifier (lowercase, no spaces)
     */
    @Override
    public String getId() {
        return "project-zomboid";
    }

    /**
     * Returns the list of authors or maintainers responsible for this provider.
     *
     * @return list of author names
     */
    @Override
    public List<String> getAuthors() {
        return List.of("The Indie Stone");
    }

    /**
     * Returns the license identifier under which the provider is distributed.
     *
     * @return SPDX identifier or short license name
     */
    @Override
    public String getLicense() {
        return "PROPRIETARY";
    }

    /**
     * Returns the list of contact references for support or source access.
     *
     * @return list of URLs, emails, or repository links
     */
    @Override
    public List<String> getContacts() {
        return List.of("https://projectzomboid.com");
    }

    /**
     * Returns the normalized version string formatted for dependency resolution.
     *
     * @return semver-compatible version
     */
    @Override
    public synchronized String getNormalizedVersion() {
        if (normalizedVersion != null) return normalizedVersion;

        String raw = getRawVersion();
        Matcher matcher = Pattern.compile("^(\\d+)\\.(\\d+)(?:\\+[^.]*)?\\.(\\d+)").matcher(raw);
        if (matcher.find()) {
            normalizedVersion = matcher.group(1) + "." + matcher.group(2) + "." + matcher.group(3);
        } else {
            log.warn("Failed to parse normalized version from '{}'. Defaulting to ??.??.??", raw);
            return "??.??.??";
        }
        return normalizedVersion;
    }

    /**
     * Returns the original version string exactly as defined by the distribution.
     *
     * @return raw version string
     */
    @Override
    public synchronized String getRawVersion() {
        if (rawVersion != null) return rawVersion;

        try (InputStream stream = classLoader.getResourceAsStream("zombie/core/Core.class")) {
            if (stream == null) throw new IOException("Class 'zombie/core/Core.class' not found");

            ClassModel model = ClassFile.of().parse(stream.readAllBytes());

            int major = 0, minor = 0;
            String extra = "";
            String patch = "0";
            String revision = null;

            // Extract GameVersion(major, minor, extra) from <clinit>
            for (MethodModel method : model.methods()) {
                if (!method.methodName().stringValue().equals("<clinit>")) continue;
                Optional<CodeModel> codeOpt = method.code();
                if (codeOpt.isEmpty()) continue;

                Deque<Object> args = new ArrayDeque<>(3);
                for (CodeElement el : codeOpt.get()) {
                    if (!(el instanceof Instruction)) continue; // skip LineNumber/LocalVariable

                    if (el instanceof ConstantInstruction ci) {
                        args.addLast(ci.constantValue());
                        if (args.size() > 3) args.removeFirst();
                    } else if (el instanceof InvokeInstruction inv) {
                        if (inv.name().stringValue().equals("<init>") &&
                                inv.owner().asInternalName().equals("zombie/core/GameVersion") &&
                                args.size() == 3) {
                            Object[] a = args.toArray();
                            if (a[0] instanceof Integer i1 && a[1] instanceof Integer i2 && a[2] instanceof String s) {
                                major = i1;
                                minor = i2;
                                extra = s;
                            }
                        }
                        args.clear();
                    } else if (!(el instanceof NewObjectInstruction || el instanceof StackInstruction || el instanceof NopInstruction)) {
                        args.clear();
                    }
                }
            }

            // Scan all methods for patch suffix and git revision constants
            for (MethodModel method : model.methods()) {
                Optional<CodeModel> codeOpt = method.code();
                if (codeOpt.isEmpty()) continue;
                for (CodeElement el : codeOpt.get()) {
                    if (el instanceof ConstantInstruction ci && ci.constantValue() instanceof String str) {
                        // Patch: e.g., ".0 ", ".1"
                        if (patch.equals("0") && str.length() >= 2 && str.charAt(0) == '.' && Character.isDigit(str.charAt(1))) {
                            patch = str.replaceAll("\\D", "");
                        }
                        // Revision: 40 hex chars + space + metadata
                        if (revision == null && str.length() > 45 && str.matches("^[0-9a-f]{40} .+")) {
                            revision = str;
                        }
                    }
                }
            }

            // Format & cache
            String extraPart = extra.isBlank() ? "" : "+" + extra;
            rawVersion = String.format("%d.%d%s.%s %s", major, minor, extraPart, patch, revision);
            return rawVersion;

        } catch (IOException e) {
            rawVersion = null;
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Returns the base working directory used for assets, configurations, and logs.
     *
     * @return the launch directory path
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
     * Returns the fully qualified class name of the main entry point to invoke.
     *
     * @return entrypoint class name
     */
    @Override
    public String getEntrypoint() {
        return isServer() ? "zombie.network.GameServer" : "zombie.gameStates.MainScreenState";
    }

    /**
     * Returns the target execution environment for this provider instance.
     *
     * @return environment type (e.g., {@code CLIENT}, {@code SERVER})
     */
    @Override
    public Environment getEnvironment() {
        return isServer() ? Environment.SERVER : Environment.CLIENT;
    }

    /**
     * Provides additional directories or JAR files to append to the runtime classpath.
     *
     * @return list of paths to Java libraries
     * @implSpec Default returns an immutable empty list.
     */
    @Override
    public List<Path> getJavaLibsPath() {
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
     * Provides directories containing platform-specific native libraries.
     *
     * @return list of paths to native library directories
     * @implSpec Default returns an immutable empty list.
     */
    @Override
    public List<Path> getNativeLibsPath() {
        Path launchDir;
        try {
            launchDir = getLaunchDirectory().toAbsolutePath().normalize();
        } catch (Exception e) {
            log.warn("Failed to resolve launch directory for native discovery in '{}'.", getId(), e);
            return List.of();
        }

        if (!Files.isDirectory(launchDir)) {
            log.debug("Launch directory is not accessible: '{}'", launchDir);
            return List.of();
        }

        Set<Path> natives = new LinkedHashSet<>();
        natives.add(launchDir);

        if (isServer()) {
            collectServerNatives(launchDir, natives);
        } else {
            collectClientNatives(launchDir, natives);
        }

        log.debug("Discovered {} native path(s) for '{}'", natives.size(), getId());
        return List.copyOf(natives);
    }

    /**
     * Scans the server-specific natives directory and its immediate subdirectories.
     *
     * @param launchDir the normalized base launch directory
     * @param natives   the mutable set to populate with discovered paths
     */
    private void collectServerNatives(Path launchDir, Set<Path> natives) {
        Path nativesRoot = launchDir.resolve("natives");
        if (!Files.isDirectory(nativesRoot)) {
            log.warn("Server natives directory not found: '{}'", nativesRoot);
            return;
        }

        natives.add(nativesRoot);
        try (var stream = Files.list(nativesRoot)) {
            stream.filter(Files::isDirectory)
                    .map(Path::toAbsolutePath)
                    .map(Path::normalize)
                    .forEach(natives::add);
        } catch (IOException e) {
            log.warn("Failed to enumerate server natives in '{}'.", nativesRoot, e);
        }
    }

    /**
     * Resolves and adds the client-specific native directory if available.
     *
     * @param launchDir the normalized base launch directory
     * @param natives   the mutable set to populate with discovered paths
     */
    private void collectClientNatives(Path launchDir, Set<Path> natives) {
        Path nativeDir = determineClientNativeDir(launchDir);
        if (nativeDir == null) return;

        if (Files.isDirectory(nativeDir)) {
            natives.add(nativeDir.toAbsolutePath().normalize());
        } else {
            log.warn("Client native directory not found: '{}'", nativeDir);
        }
    }

    /**
     * Provides internal key-value configuration arguments injected before launch.
     *
     * @return map of provider-specific arguments
     * @implSpec Default returns an immutable empty map.
     */
    @Override
    public Map<String, String> getProviderArgs() {
        if (isServer()) {
            Map<String, String> serverArgs = new HashMap<>(BASE_CORE_ARGS);
            serverArgs.put("java.awt.headless", "true");
            return serverArgs;
        }
        return BASE_CORE_ARGS;
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
        if (isStreamRedirected != null) return;

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

            isStreamRedirected = true;
        } catch (Exception e) {
            log.error("Failed to redirect system streams for '{}'.", getId(), e);
            throw new IllegalStateException("Failed to redirect system streams for: " + getId(), e);
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

        log.debug("Unknown OS '{}'. Client native path cannot be determined.", os);
        return null;
    }

    /**
     * Determines whether this installation should be treated as a dedicated server.
     *
     * @return {@code true} if this is a server installation, otherwise {@code false}
     */
    public boolean isServer() {
        if (isServerDetect != null) return isServerDetect;

        try {
            isServerDetect = Files.isDirectory(getLaunchDirectory().resolve("java"));
        } catch (Exception e) {
            log.warn("Failed to detect server mode for '{}'. Defaulting to CLIENT.", getId(), e);
            isServerDetect = false;
        }

        return isServerDetect;
    }
}
