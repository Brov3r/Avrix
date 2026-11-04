package com.avrix.provider;

import com.avrix.core.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Detects whether a Project Zomboid installation represents a client or a dedicated server.
 * <p>
 * Detection prefers explicit configuration and launch metadata over file- and directory-name heuristics.
 */
final class ZomboidEnvironmentDetector {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZomboidEnvironmentDetector.class);

    private static final String CLIENT_MAIN_CLASS = "zombie/gamestates/mainscreenstate";
    private static final String SERVER_MAIN_CLASS = "zombie/network/gameserver";

    private static final List<String> WINDOWS_SERVER_MARKERS = List.of(
            "startserver32.bat",
            "startserver64.bat",
            "startserver64_nosteam.bat",
            "startserver32_nosteam.bat"
    );

    private static final List<String> LINUX_SERVER_MARKERS = List.of(
            "start-server.sh",
            "start-server-nosteam.sh",
            "startserver64.sh",
            "startserver64_nosteam.sh"
    );

    private static final List<String> WINDOWS_CLIENT_MARKERS = List.of(
            "projectzomboid64.exe",
            "projectzomboid32.exe",
            "projectzomboid64.bat",
            "projectzomboid32.bat"
    );

    private static final List<String> LINUX_CLIENT_MARKERS = List.of(
            "projectzomboid64.sh",
            "projectzomboid.sh",
            "projectzomboid64",
            "projectzomboid32"
    );

    private static final List<String> MAC_CLIENT_MARKERS = List.of(
            "projectzomboid64.sh",
            "projectzomboid.sh",
            "projectzomboid64",
            "projectzomboid32"
    );

    private static final List<String> CLIENT_NATIVE_DIRECTORIES = List.of(
            "win64", "win32", "winarm64",
            "linux64", "linux32", "linux-arm64", "linux-aarch64",
            "mac64", "macarm64", "macos", "osx"
    );

    private static final Pattern MAIN_CLASS_PATTERN = Pattern.compile(
            "\"mainClass\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\""
    );

    private static final Pattern INSTALL_DIR_PATTERN = Pattern.compile(
            "\"installdir\"\\s+\"([^\"]*)\""
    );

    private final String osName;

    /**
     * Constructs a detector for the current JVM operating system.
     */
    ZomboidEnvironmentDetector() {
        this(System.getProperty("os.name", ""));
    }

    /**
     * Constructs a detector for the provided operating-system name.
     *
     * @param osName operating-system name, for example {@code Windows 11} or {@code Linux}
     */
    ZomboidEnvironmentDetector(String osName) {
        this.osName = (osName == null ? "" : osName).toLowerCase(Locale.ROOT);
    }

    /**
     * Detects the environment of the provided Project Zomboid installation.
     *
     * @param launchDirectory game installation root
     * @return detected environment, never {@code null}
     */
    Environment detect(Path launchDirectory) {
        Environment explicitEnvironment = detectExplicitOverride();
        if (explicitEnvironment != null) {
            LOGGER.info("Using explicitly configured game environment [{}]", explicitEnvironment);
            return explicitEnvironment;
        }

        Map<String, Boolean> signals = new LinkedHashMap<>();
        signals.put("launch config", detectFromLaunchConfig(launchDirectory));
        signals.put("launch script", detectFromLaunchScripts(launchDirectory));
        signals.put("marker file", detectFromMarkerFiles(launchDirectory));
        signals.put("installation layout", detectFromStructure(launchDirectory));
        signals.put("Steam manifest", detectFromSteamManifest(launchDirectory));
        signals.put("directory name", detectFromDirectoryName(launchDirectory));

        String selectedSource = null;
        Boolean selectedEnvironment = null;
        for (Map.Entry<String, Boolean> signal : signals.entrySet()) {
            if (signal.getValue() != null) {
                selectedSource = signal.getKey();
                selectedEnvironment = signal.getValue();
                break;
            }
        }

        if (selectedEnvironment == null) {
            LOGGER.warn(
                    "Could not determine client/server topology for [{}]. Falling back to CLIENT.",
                    launchDirectory
            );
            return Environment.CLIENT;
        }

        for (Map.Entry<String, Boolean> signal : signals.entrySet()) {
            if (signal.getValue() != null && !signal.getValue().equals(selectedEnvironment)) {
                LOGGER.warn(
                        "Conflicting client/server signals in [{}]: selected [{}] from [{}], ignored [{}] from [{}]",
                        launchDirectory,
                        environmentName(selectedEnvironment),
                        selectedSource,
                        environmentName(signal.getValue()),
                        signal.getKey()
                );
            }
        }

        Environment environment = selectedEnvironment ? Environment.SERVER : Environment.CLIENT;
        LOGGER.debug(
                "Detected game environment [{}] for [{}] from [{}]",
                environment,
                launchDirectory,
                selectedSource
        );
        return environment;
    }

    /**
     * Resolves a macOS application bundle to the directory containing the game files.
     * <p>
     * Non-bundle paths and bundles without recognizable game files are returned unchanged.
     *
     * @param candidate candidate game root or path inside an application bundle
     * @return normalized directory containing the game files, or the normalized input when no bundle is found
     */
    static Path resolveMacBundleGameRoot(Path candidate) {
        Path normalized = candidate.toAbsolutePath().normalize();
        Path bundle = findAppBundle(normalized);
        if (bundle == null) {
            return normalized;
        }

        List<Path> candidates = List.of(
                bundle.resolve("Contents").resolve("Resources").resolve("Java"),
                bundle.resolve("Contents").resolve("Resources"),
                bundle.resolve("Contents").resolve("MacOS"),
                bundle
        );

        for (Path gameRoot : candidates) {
            if (looksLikeGameRoot(gameRoot)) {
                return gameRoot.toAbsolutePath().normalize();
            }
        }

        return normalized;
    }

    private static Environment detectExplicitOverride() {
        String raw = System.getProperty("avrix.environment");
        if (raw == null || raw.isBlank()) {
            raw = System.getenv("AVRIX_ENVIRONMENT");
        }
        if (raw == null || raw.isBlank()) {
            return null;
        }

        return switch (raw.trim().toLowerCase(Locale.ROOT)) {
            case "server", "dedicated", "dedicated-server", "dedicated_server" -> Environment.SERVER;
            case "client", "desktop" -> Environment.CLIENT;
            default -> {
                LOGGER.warn("Unknown explicit game environment [{}]. Ignoring override.", raw);
                yield null;
            }
        };
    }

    private Boolean detectFromLaunchConfig(Path launchDirectory) {
        for (Path config : findLaunchConfigs(launchDirectory)) {
            String content;
            try {
                content = Files.readString(config, StandardCharsets.UTF_8);
            } catch (IOException e) {
                LOGGER.debug("Failed to read launch configuration [{}]", config, e);
                continue;
            }

            Environment mainClassEnvironment = environmentForMainClass(extractJsonString(content, MAIN_CLASS_PATTERN));
            if (mainClassEnvironment != null) {
                LOGGER.debug("Detected [{}] from mainClass in [{}]", mainClassEnvironment, config);
                return mainClassEnvironment == Environment.SERVER;
            }

            Boolean classpathEnvironment = detectFromClasspath(content);
            if (classpathEnvironment != null) {
                LOGGER.debug("Detected [{}] from classpath in [{}]", environmentName(classpathEnvironment), config);
                return classpathEnvironment;
            }
        }
        return null;
    }

    private static List<Path> findLaunchConfigs(Path launchDirectory) {
        List<Path> configs = new ArrayList<>();
        addLaunchConfigCandidates(configs, launchDirectory);
        addLaunchConfigCandidates(configs, launchDirectory.resolve("java"));
        addLaunchConfigCandidates(configs, launchDirectory.resolve("Contents").resolve("Resources").resolve("Java"));
        addLaunchConfigCandidates(configs, launchDirectory.resolve("Contents").resolve("Resources"));
        addLaunchConfigCandidates(configs, launchDirectory.resolve("Contents").resolve("MacOS"));

        if (Files.isDirectory(launchDirectory)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(launchDirectory)) {
                for (Path entry : stream) {
                    if (!Files.isDirectory(entry) || !endsWithIgnoreCase(entry, ".app")) {
                        continue;
                    }
                    addLaunchConfigCandidates(configs, entry.resolve("Contents").resolve("Resources").resolve("Java"));
                    addLaunchConfigCandidates(configs, entry.resolve("Contents").resolve("Resources"));
                    addLaunchConfigCandidates(configs, entry.resolve("Contents").resolve("MacOS"));
                }
            } catch (IOException e) {
                LOGGER.debug("Failed to search for macOS launch configurations in [{}]", launchDirectory, e);
            }
        }

        return configs;
    }

    private static void addLaunchConfigCandidates(List<Path> configs, Path directory) {
        addIfRegularFile(configs, directory.resolve("ProjectZomboid64.json"));
        addIfRegularFile(configs, directory.resolve("ProjectZomboid32.json"));
    }

    private static void addIfRegularFile(List<Path> paths, Path candidate) {
        if (Files.isRegularFile(candidate) && !paths.contains(candidate)) {
            paths.add(candidate);
        }
    }

    private static String extractJsonString(String content, Pattern pattern) {
        Matcher matcher = pattern.matcher(content);
        if (!matcher.find()) {
            return null;
        }
        return matcher.group(1)
                .replace("\\\\", "/")
                .replace("\\/", "/")
                .replace('\\', '/')
                .trim();
    }

    private static Environment environmentForMainClass(String rawMainClass) {
        if (rawMainClass == null || rawMainClass.isBlank()) {
            return null;
        }

        String normalized = rawMainClass.toLowerCase(Locale.ROOT)
                .replace('\\', '/')
                .replace('.', '/')
                .trim();

        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        while (normalized.contains("//")) {
            normalized = normalized.replace("//", "/");
        }
        if (normalized.endsWith(".class")) {
            normalized = normalized.substring(0, normalized.length() - ".class".length());
        }

        if (normalized.equals(SERVER_MAIN_CLASS)) {
            return Environment.SERVER;
        }
        if (normalized.equals(CLIENT_MAIN_CLASS)) {
            return Environment.CLIENT;
        }
        return null;
    }

    private static Boolean detectFromClasspath(String content) {
        String normalized = content.toLowerCase(Locale.ROOT).replace('\\', '/');
        if (normalized.contains("\"java/projectzomboid.jar\"")) {
            return true;
        }
        if (normalized.contains("\"projectzomboid.jar\"")) {
            return false;
        }
        return null;
    }

    private Boolean detectFromLaunchScripts(Path launchDirectory) {
        Boolean detected = null;

        for (Path script : findLaunchScripts(launchDirectory)) {
            String content;
            try {
                content = Files.readString(script, StandardCharsets.UTF_8);
            } catch (IOException e) {
                LOGGER.debug("Failed to read launch script [{}]", script, e);
                continue;
            }

            Boolean scriptEnvironment = detectFromScriptContent(content);
            if (scriptEnvironment == null) {
                continue;
            }
            if (detected != null && !detected.equals(scriptEnvironment)) {
                LOGGER.warn("Launch scripts in [{}] disagree about client/server topology", launchDirectory);
                return null;
            }

            detected = scriptEnvironment;
            LOGGER.debug(
                    "Detected [{}] from launch script [{}]",
                    environmentName(scriptEnvironment),
                    script
            );
        }

        return detected;
    }

    private static List<Path> findLaunchScripts(Path launchDirectory) {
        List<Path> scripts = new ArrayList<>();
        if (!Files.isDirectory(launchDirectory)) {
            return scripts;
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(launchDirectory)) {
            for (Path entry : stream) {
                if (!Files.isRegularFile(entry)) {
                    continue;
                }

                String name = entry.getFileName().toString().toLowerCase(Locale.ROOT);
                if (!name.endsWith(".bat") && !name.endsWith(".sh") && !name.endsWith(".command")) {
                    continue;
                }
                if (isServerScriptName(name) || isClientScriptName(name)) {
                    scripts.add(entry);
                }
            }
        } catch (IOException e) {
            LOGGER.debug("Failed to enumerate launch scripts in [{}]", launchDirectory, e);
        }

        return scripts;
    }

    private static boolean isServerScriptName(String name) {
        return name.startsWith("startserver") || name.startsWith("start-server");
    }

    private static boolean isClientScriptName(String name) {
        // The client distribution contains ProjectZomboidServer.bat, which starts GameServer.
        return name.startsWith("projectzomboid") && !name.contains("server");
    }

    private static Boolean detectFromScriptContent(String content) {
        String normalized = content.toLowerCase(Locale.ROOT).replace('\\', '/');
        boolean server = normalized.contains("zombie/network/gameserver")
                || normalized.contains("zombie.network.gameserver");
        boolean client = normalized.contains("zombie/gamestates/mainscreenstate")
                || normalized.contains("zombie.gamestates.mainscreenstate");

        if (server == client) {
            return null;
        }
        return server;
    }

    private Boolean detectFromMarkerFiles(Path launchDirectory) {
        boolean server = hasServerMarker(launchDirectory);
        boolean client = hasClientMarker(launchDirectory);

        if (server && client) {
            LOGGER.warn("Both client and server marker files found in [{}]; preferring SERVER", launchDirectory);
        }
        if (server) {
            return true;
        }
        if (client) {
            return false;
        }
        return null;
    }

    boolean hasServerMarker(Path launchDirectory) {
        return containsAnyFileName(launchDirectory, serverMarkers());
    }

    boolean hasClientMarker(Path launchDirectory) {
        return containsAnyFileName(launchDirectory, clientMarkers());
    }

    private List<String> serverMarkers() {
        if (osName.contains("win")) {
            return WINDOWS_SERVER_MARKERS;
        }
        if (osName.contains("linux")) {
            return LINUX_SERVER_MARKERS;
        }
        if (osName.contains("mac")) {
            return List.of();
        }
        return combined(WINDOWS_SERVER_MARKERS, LINUX_SERVER_MARKERS);
    }

    private List<String> clientMarkers() {
        if (osName.contains("win")) {
            return WINDOWS_CLIENT_MARKERS;
        }
        if (osName.contains("linux")) {
            return LINUX_CLIENT_MARKERS;
        }
        if (osName.contains("mac")) {
            return MAC_CLIENT_MARKERS;
        }
        return combined(combined(WINDOWS_CLIENT_MARKERS, LINUX_CLIENT_MARKERS), MAC_CLIENT_MARKERS);
    }

    private static List<String> combined(List<String> first, List<String> second) {
        List<String> combined = new ArrayList<>(first.size() + second.size());
        combined.addAll(first);
        combined.addAll(second);
        return List.copyOf(combined);
    }

    private static boolean containsAnyFileName(Path directory, List<String> names) {
        if (!Files.isDirectory(directory)) {
            return false;
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory)) {
            for (Path entry : stream) {
                if (!Files.isRegularFile(entry)) {
                    continue;
                }
                String name = entry.getFileName().toString().toLowerCase(Locale.ROOT);
                if (names.contains(name)) {
                    return true;
                }
            }
        } catch (IOException e) {
            LOGGER.debug("Failed to inspect marker files in [{}]", directory, e);
        }
        return false;
    }

    private Boolean detectFromStructure(Path launchDirectory) {
        boolean server = looksLikeServerLayout(launchDirectory);
        boolean client = looksLikeClientLayout(launchDirectory);

        if (server && client) {
            LOGGER.warn("Installation layout in [{}] matches both client and server", launchDirectory);
            return null;
        }
        if (server) {
            return true;
        }
        if (client) {
            return false;
        }
        return null;
    }

    /**
     * Checks whether a directory contains the dedicated-server classpath and native libraries.
     *
     * @param launchDirectory candidate installation root
     * @return {@code true} when the directory looks like a dedicated-server installation
     */
    static boolean looksLikeServerLayout(Path launchDirectory) {
        if (!Files.isRegularFile(launchDirectory.resolve("java").resolve("projectzomboid.jar"))) {
            return false;
        }
        return hasNativeLibraries(launchDirectory.resolve("natives"));
    }

    /**
     * Checks whether a directory contains the client game archive and platform-native artifacts.
     *
     * @param launchDirectory candidate installation root
     * @return {@code true} when the directory looks like a client installation
     */
    static boolean looksLikeClientLayout(Path launchDirectory) {
        if (looksLikeMacBundle(launchDirectory)) {
            return true;
        }
        if (!Files.isRegularFile(launchDirectory.resolve("projectzomboid.jar"))) {
            return false;
        }
        if (Files.isDirectory(launchDirectory.resolve("jre64"))) {
            return true;
        }
        for (String directory : CLIENT_NATIVE_DIRECTORIES) {
            if (Files.isDirectory(launchDirectory.resolve(directory))) {
                return true;
            }
        }
        return hasNativeLibraries(launchDirectory);
    }

    /**
     * Checks whether a path points to a macOS application bundle containing game files.
     *
     * @param launchDirectory candidate installation root
     * @return {@code true} when the path is or contains an application bundle
     */
    static boolean looksLikeMacBundle(Path launchDirectory) {
        if (launchDirectory == null) {
            return false;
        }
        if (endsWithIgnoreCase(launchDirectory, ".app")) {
            return true;
        }
        if (Files.isRegularFile(launchDirectory.resolve("Contents").resolve("Info.plist"))) {
            return true;
        }

        if (Files.isDirectory(launchDirectory)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(launchDirectory)) {
                for (Path entry : stream) {
                    if (Files.isDirectory(entry) && endsWithIgnoreCase(entry, ".app")) {
                        return true;
                    }
                }
            } catch (IOException e) {
                LOGGER.debug("Failed to inspect macOS application bundles in [{}]", launchDirectory, e);
            }
        }
        return false;
    }

    private static boolean hasNativeLibraries(Path directory) {
        if (!Files.isDirectory(directory)) {
            return false;
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory)) {
            for (Path entry : stream) {
                if (Files.isRegularFile(entry)) {
                    if (isNativeLibrary(entry)) {
                        return true;
                    }
                    continue;
                }

                if (!Files.isDirectory(entry)) {
                    continue;
                }

                try (DirectoryStream<Path> nested = Files.newDirectoryStream(entry)) {
                    for (Path nestedEntry : nested) {
                        if (Files.isRegularFile(nestedEntry) && isNativeLibrary(nestedEntry)) {
                            return true;
                        }
                    }
                } catch (IOException _) {
                    // Native discovery is best-effort; keep checking other entries.
                }
            }
        } catch (IOException e) {
            LOGGER.debug("Failed to inspect native libraries in [{}]", directory, e);
        }
        return false;
    }

    private static boolean isNativeLibrary(Path path) {
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return name.endsWith(".dll") || name.endsWith(".so") || name.endsWith(".dylib");
    }

    private Boolean detectFromSteamManifest(Path launchDirectory) {
        Path steamApps = findSteamAppsParent(launchDirectory);
        Path directoryName = launchDirectory.getFileName();
        if (steamApps == null || directoryName == null) {
            return null;
        }

        String installDirectory = directoryName.toString();
        boolean client = matchesInstallDirectory(steamApps.resolve("appmanifest_108600.acf"), installDirectory);
        boolean server = matchesInstallDirectory(steamApps.resolve("appmanifest_380870.acf"), installDirectory);

        if (client && server) {
            LOGGER.warn("Steam manifests in [{}] both match [{}]", steamApps, installDirectory);
            return null;
        }
        if (client) {
            return false;
        }
        if (server) {
            return true;
        }
        return null;
    }

    private static Path findSteamAppsParent(Path launchDirectory) {
        Path current = launchDirectory.toAbsolutePath().normalize();
        while (current != null) {
            Path name = current.getFileName();
            if (name != null && name.toString().equalsIgnoreCase("steamapps")) {
                return current;
            }
            current = current.getParent();
        }
        return null;
    }

    private static boolean matchesInstallDirectory(Path manifest, String installDirectory) {
        if (!Files.isRegularFile(manifest)) {
            return false;
        }

        try {
            String content = Files.readString(manifest, StandardCharsets.UTF_8);
            Matcher matcher = INSTALL_DIR_PATTERN.matcher(content);
            return matcher.find() && matcher.group(1).equalsIgnoreCase(installDirectory);
        } catch (IOException e) {
            LOGGER.debug("Failed to read Steam manifest [{}]", manifest, e);
            return false;
        }
    }

    private static Boolean detectFromDirectoryName(Path launchDirectory) {
        Path directoryName = launchDirectory.getFileName();
        if (directoryName == null) {
            return null;
        }

        String name = directoryName.toString().toLowerCase(Locale.ROOT);
        if (name.contains("dedicated server")
                || name.contains("dedicatedserver")
                || name.contains("pzserver")
                || name.contains("pz-server")) {
            return true;
        }
        if (name.contains("projectzomboid") || name.contains("project zomboid")) {
            return false;
        }
        return null;
    }

    private static boolean looksLikeGameRoot(Path directory) {
        return Files.isRegularFile(directory.resolve("projectzomboid.jar"))
                || Files.isRegularFile(directory.resolve("java").resolve("projectzomboid.jar"))
                || Files.isRegularFile(directory.resolve("ProjectZomboid64.json"))
                || Files.isRegularFile(directory.resolve("ProjectZomboid32.json"));
    }

    private static Path findAppBundle(Path path) {
        Path current = path;
        while (current != null) {
            if (endsWithIgnoreCase(current, ".app")) {
                return current;
            }
            current = current.getParent();
        }
        return null;
    }

    private static boolean endsWithIgnoreCase(Path path, String suffix) {
        Path fileName = path.getFileName();
        return fileName != null && fileName.toString().toLowerCase(Locale.ROOT).endsWith(suffix);
    }

    private static String environmentName(boolean server) {
        return server ? "SERVER" : "CLIENT";
    }
}
