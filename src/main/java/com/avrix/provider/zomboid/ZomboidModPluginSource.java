package com.avrix.provider.zomboid;

import com.avrix.api.PluginArchive;
import com.avrix.api.PluginSource;
import com.avrix.core.Environment;
import com.github.zafarkhaja.semver.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Discovers plugin archives declared by active Project Zomboid mods through the {@code jarPlugins} manifest key.
 * <p>
 * The scanner reads the active mod list from the client {@code default.txt} file or the dedicated-server
 * {@code Server/<name>.ini} file, then inspects only matching mod directories. It never scans arbitrary JAR files.
 */
public final class ZomboidModPluginSource implements PluginSource {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZomboidModPluginSource.class);

    private static final String ZOMBOID_FOLDER = "Zomboid";
    private static final String MODS_FOLDER = "mods";
    private static final String SERVER_FOLDER = "Server";
    private static final String COMMON_DIRECTORY = "common";
    private static final String VERSION_DIRECTORY_PATTERN = "42*";
    private static final String JAR_SEPARATOR_PATTERN = "[;,]";
    private static final Pattern WORKSHOP_ITEM_PATTERN =
            Pattern.compile("/content/" + ZomboidConstants.WORKSHOP_APP_ID + "/(\\d+)/");

    private final Path gameRoot;
    private final Path cacheRoot;
    private final Environment environment;
    private final String serverName;
    private final Version gameVersion;
    private final boolean strict;

    /**
     * Creates a scanner using the current user home.
     *
     * @param gameRoot    Project Zomboid installation root
     * @param environment active runtime environment
     * @param serverName  dedicated-server name, or {@code null} to use the default
     */
    public ZomboidModPluginSource(Path gameRoot, Environment environment, String serverName) {
        this(gameRoot, Path.of(System.getProperty("user.home", ".")), environment, serverName, null, false);
    }

    /**
     * Creates a scanner with an explicit user home, primarily for tests.
     *
     * @param gameRoot    Project Zomboid installation root
     * @param userHome    user home directory containing the {@code Zomboid} folder
     * @param environment active runtime environment
     * @param serverName  dedicated-server name, or {@code null} to use the default
     */
    public ZomboidModPluginSource(Path gameRoot, Path userHome, Environment environment, String serverName) {
        this(gameRoot, userHome, environment, serverName, null, false);
    }

    /**
     * Creates a scanner using the current user home.
     *
     * @param gameRoot    Project Zomboid installation root
     * @param environment active runtime environment
     * @param serverName  dedicated-server name, or {@code null} to use the default
     * @param gameVersion normalized game version, or {@code null} when unknown
     * @param strict      whether discovery errors should abort startup
     */
    public ZomboidModPluginSource(
            Path gameRoot,
            Environment environment,
            String serverName,
            String gameVersion,
            boolean strict
    ) {
        this(gameRoot, Path.of(System.getProperty("user.home", ".")), environment, serverName, gameVersion, strict);
    }

    /**
     * Creates a scanner with an explicit user home, primarily for tests.
     *
     * @param gameRoot    Project Zomboid installation root
     * @param userHome    user home directory containing the {@code Zomboid} folder
     * @param environment active runtime environment
     * @param serverName  dedicated-server name, or {@code null} to use the default
     * @param gameVersion normalized game version, or {@code null} when unknown
     * @param strict      whether discovery errors should abort startup
     */
    public ZomboidModPluginSource(
            Path gameRoot,
            Path userHome,
            Environment environment,
            String serverName,
            String gameVersion,
            boolean strict
    ) {
        this(gameRoot, environment, serverName, gameVersion, strict,
                Objects.requireNonNull(userHome, "User home cannot be null").resolve(ZOMBOID_FOLDER));
    }

    /**
     * Creates a scanner rooted in the game's resolved cache directory.
     *
     * @param gameRoot game installation root
     * @param cacheRoot directory containing {@code mods} and {@code Server}, including any cache override
     * @param environment active runtime environment
     * @param serverName server profile name, or {@code null} for the default
     * @param gameVersion normalized game version, or {@code null} when unknown
     * @param strict whether discovery errors should abort startup
     * @return scanner using the specified cache root without appending a folder name
     * @throws NullPointerException if either root is null
     */
    public static ZomboidModPluginSource forCacheRoot(
            Path gameRoot, Path cacheRoot, Environment environment, String serverName, String gameVersion, boolean strict
    ) {
        return new ZomboidModPluginSource(gameRoot, environment, serverName, gameVersion, strict, cacheRoot);
    }

    private ZomboidModPluginSource(
            Path gameRoot, Environment environment, String serverName, String gameVersion, boolean strict, Path cacheRoot
    ) {
        this.gameRoot = Objects.requireNonNull(gameRoot, "Game root cannot be null")
                .toAbsolutePath()
                .normalize();
        this.cacheRoot = Objects.requireNonNull(cacheRoot, "Cache root cannot be null")
                .toAbsolutePath()
                .normalize();
        this.environment = environment == null ? Environment.CLIENT : environment;
        this.serverName = (serverName == null || serverName.isBlank())
                ? ZomboidConstants.DEFAULT_SERVER_NAME
                : serverName.trim();
        this.gameVersion = parseVersion(gameVersion);
        this.strict = strict;
    }

    /**
     * Scans active mod directories for {@code jarPlugins} entries.
     *
     * @return normalized paths to discovered plugin archives
     * @throws IllegalStateException when strict mode is enabled and discovery errors were collected
     */
    public List<Path> scan() {
        return scanArchives().stream()
                .map(archive -> archive.file().toPath())
                .toList();
    }

    /**
     * Scans active mod directories for {@code jarPlugins} entries with source metadata.
     *
     * @return discovered plugin archives
     * @throws IllegalStateException when strict mode is enabled and discovery errors were collected
     */
    @Override
    public List<PluginArchive> scanArchives() {
        Set<String> activeMods = readActiveMods();
        if (activeMods.isEmpty()) {
            LOGGER.debug("No active Project Zomboid mods found. Skipping mod.info plugin discovery.");
            return List.of();
        }

        Map<Path, PluginArchive> archives = new LinkedHashMap<>();
        Set<String> errors = new LinkedHashSet<>();
        scanSteamWorkshop(activeMods, archives, errors);
        scanLocalMods(activeMods, archives, errors);

        if (strict && !errors.isEmpty()) {
            throw new IllegalStateException(
                    "Failed to discover Avrix plugins:\n - " + String.join("\n - ", errors)
            );
        }

        if (!archives.isEmpty()) {
            LOGGER.debug("Discovered {} plugin archive(s) from {} active mod(s).", archives.size(), activeMods.size());
        }
        return List.copyOf(archives.values());
    }

    private Set<String> readActiveMods() {
        if (environment == Environment.SERVER) {
            Path serverConfig = cacheRoot
                    .resolve(SERVER_FOLDER)
                    .resolve(serverName + ".ini");
            return readServerActiveMods(serverConfig);
        }

        Path clientConfig = cacheRoot
                .resolve(MODS_FOLDER)
                .resolve(ZomboidConstants.ACTIVE_MODS_FILE_NAME);
        return readClientActiveMods(clientConfig);
    }

    private static Set<String> readClientActiveMods(Path file) {
        if (!Files.isRegularFile(file)) {
            LOGGER.debug("Active client mod list not found: [{}]", file);
            return Set.of();
        }

        Set<String> activeMods = new HashSet<>();
        boolean inModsBlock = false;

        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            boolean firstLine = true;

            while ((line = reader.readLine()) != null) {
                if (firstLine) {
                    line = stripBom(line);
                    firstLine = false;
                }

                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }

                if (!inModsBlock) {
                    if (startsWithIgnoreCase(trimmed, "mods")) {
                        char next = trimmed.length() > 4 ? trimmed.charAt(4) : ' ';
                        if (Character.isWhitespace(next) || next == '{') {
                            inModsBlock = true;
                        }
                    }
                    continue;
                }

                if (trimmed.startsWith("}")) {
                    inModsBlock = false;
                    continue;
                }

                int separator = trimmed.indexOf('=');
                if (separator < 0) {
                    continue;
                }

                String key = trimmed.substring(0, separator).trim();
                if (!key.equalsIgnoreCase("mod")) {
                    continue;
                }

                String modId = stripTrailingComma(trimmed.substring(separator + 1));
                if (!modId.isEmpty()) {
                    activeMods.add(modId.toLowerCase(Locale.ROOT));
                }
            }
        } catch (IOException e) {
            LOGGER.debug("Failed to read active client mod list [{}]", file, e);
            return Set.of();
        }

        return activeMods;
    }

    private static Set<String> readServerActiveMods(Path file) {
        if (!Files.isRegularFile(file)) {
            LOGGER.debug("Dedicated-server configuration not found: [{}]", file);
            return Set.of();
        }

        Set<String> activeMods = new HashSet<>();

        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            boolean firstLine = true;

            while ((line = reader.readLine()) != null) {
                if (firstLine) {
                    line = stripBom(line);
                    firstLine = false;
                }

                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }

                int separator = trimmed.indexOf('=');
                if (separator < 0) {
                    continue;
                }

                String key = trimmed.substring(0, separator).trim();
                if (!key.equalsIgnoreCase("Mods")) {
                    continue;
                }

                for (String entry : trimmed.substring(separator + 1).split(JAR_SEPARATOR_PATTERN)) {
                    String modId = entry.trim();
                    if (!modId.isEmpty()) {
                        activeMods.add(modId.toLowerCase(Locale.ROOT));
                    }
                }
                break;
            }
        } catch (IOException e) {
            LOGGER.debug("Failed to read dedicated-server configuration [{}]", file, e);
            return Set.of();
        }

        return activeMods;
    }

    private void scanSteamWorkshop(
            Set<String> activeMods,
            Map<Path, PluginArchive> archives,
            Set<String> errors
    ) {
        Path steamApps = findSteamApps();
        if (steamApps == null) {
            return;
        }

        Path workshopContent = steamApps
                .resolve("workshop")
                .resolve("content")
                .resolve(ZomboidConstants.WORKSHOP_APP_ID);
        if (!Files.isDirectory(workshopContent)) {
            return;
        }

        try (DirectoryStream<Path> items = Files.newDirectoryStream(workshopContent)) {
            for (Path item : items) {
                if (!Files.isDirectory(item)) {
                    continue;
                }

                Path modsRoot = item.resolve(MODS_FOLDER);
                if (Files.isDirectory(modsRoot)) {
                    scanModDirectories(modsRoot, activeMods, archives, errors);
                }
            }
        } catch (IOException e) {
            LOGGER.debug("Failed to scan Workshop content [{}]", workshopContent, e);
        }
    }

    private void scanLocalMods(
            Set<String> activeMods,
            Map<Path, PluginArchive> archives,
            Set<String> errors
    ) {
        Path localMods = cacheRoot.resolve(MODS_FOLDER);
        scanModDirectories(localMods, activeMods, archives, errors);
    }

    private void scanModDirectories(
            Path modsRoot,
            Set<String> activeMods,
            Map<Path, PluginArchive> archives,
            Set<String> errors
    ) {
        if (!Files.isDirectory(modsRoot)) {
            return;
        }

        try (DirectoryStream<Path> mods = Files.newDirectoryStream(modsRoot)) {
            for (Path modDirectory : mods) {
                if (Files.isDirectory(modDirectory)) {
                    scanMod(modDirectory, activeMods, archives, errors);
                }
            }
        } catch (IOException e) {
            LOGGER.debug("Failed to scan mod directory [{}]", modsRoot, e);
        }
    }

    private void scanMod(
            Path modDirectory,
            Set<String> activeMods,
            Map<Path, PluginArchive> archives,
            Set<String> errors
    ) {
        Path commonDirectory = modDirectory.resolve(COMMON_DIRECTORY);
        List<Path> versionDirectories = versionDirectories(modDirectory);
        Path selectedDirectory = versionDirectories.isEmpty() ? null : versionDirectories.get(0);
        Path versionFallback = selectedDirectory != null ? selectedDirectory : commonDirectory;

        ModInfo selectedInfo = selectedDirectory == null
                ? null
                : parseModInfo(selectedDirectory.resolve("mod.info"), selectedDirectory);

        ModInfo commonInfo = Files.isDirectory(commonDirectory)
                ? parseModInfo(commonDirectory.resolve("mod.info"), commonDirectory)
                : null;

        if (selectedInfo == null && commonInfo == null) {
            return;
        }

        String modId = firstNonBlank(
                selectedInfo != null ? selectedInfo.id() : null,
                commonInfo != null ? commonInfo.id() : null,
                modDirectory.getFileName().toString()
        );
        String modName = firstNonBlank(
                selectedInfo != null ? selectedInfo.name() : null,
                commonInfo != null ? commonInfo.name() : null,
                modId
        );
        String modAuthor = firstNonBlank(
                selectedInfo != null ? selectedInfo.author() : null,
                commonInfo != null ? commonInfo.author() : null,
                ""
        );
        String workshopId = workshopItemId(modDirectory);

        if (!activeMods.contains(modId.toLowerCase(Locale.ROOT))) {
            return;
        }

        Path realModRoot;
        try {
            realModRoot = modDirectory.toRealPath();
        } catch (IOException e) {
            LOGGER.debug("Failed to resolve mod directory [{}]", modDirectory, e);
            return;
        }

        int discovered = 0;
        boolean selectedDeclaresJars = selectedInfo != null && !selectedInfo.jarEntries().isEmpty();

        if (selectedInfo != null) {
            Path selectedFallback = selectedDirectory.equals(commonDirectory)
                    ? versionFallback
                    : commonDirectory;
            discovered += addJarCandidates(
                    selectedInfo, selectedFallback, realModRoot, modId, modName, modAuthor, workshopId, archives, errors
            );
        }

        // A version-specific mod.info overrides common jar declarations. common remains the fallback
        // when the selected version does not declare any jarPlugins entries.
        if (!selectedDeclaresJars && commonInfo != null && !commonDirectory.equals(selectedDirectory)) {
            Path commonFallback = selectedDirectory != null ? selectedDirectory : versionFallback;
            discovered += addJarCandidates(
                    commonInfo, commonFallback, realModRoot, modId, modName, modAuthor, workshopId, archives, errors
            );
        }

        if (discovered > 0) {
            LOGGER.info(
                    "Discovered {} plugin archive(s) from {} [{}] at [{}]",
                    discovered,
                    workshopId != null ? "Workshop item " + workshopId : "local mod",
                    modId,
                    modDirectory
            );
        }
    }

    /**
     * Selects a version directory using the game version.
     * <p>
     * The highest directory at or below the game version wins. When every directory is newer than the game,
     * the nearest newer directory is used. If the game version is unknown, the newest directory is selected.
     *
     * @param modDirectory mod root directory
     * @return zero or one selected version directory
     */
    private List<Path> versionDirectories(Path modDirectory) {
        List<VersionedDirectory> candidates = new ArrayList<>(3);

        try (DirectoryStream<Path> versions = Files.newDirectoryStream(modDirectory, VERSION_DIRECTORY_PATTERN)) {
            for (Path version : versions) {
                if (!Files.isDirectory(version)) {
                    continue;
                }

                Version parsed = parseVersion(version.getFileName().toString());
                if (parsed != null) {
                    candidates.add(new VersionedDirectory(version, parsed));
                }
            }
        } catch (IOException e) {
            LOGGER.debug("Failed to enumerate version directories in [{}]", modDirectory, e);
        }

        if (candidates.isEmpty()) {
            return List.of();
        }

        candidates.sort(Comparator.comparing(VersionedDirectory::version));

        VersionedDirectory selected = null;
        if (gameVersion != null) {
            for (VersionedDirectory candidate : candidates) {
                if (candidate.version().compareTo(gameVersion) <= 0) {
                    selected = candidate;
                }
            }

            if (selected == null) {
                for (VersionedDirectory candidate : candidates) {
                    if (candidate.version().compareTo(gameVersion) > 0) {
                        selected = candidate;
                        break;
                    }
                }
            }
        }

        if (selected == null) {
            selected = candidates.get(candidates.size() - 1);
        }

        return List.of(selected.directory());
    }

    private static ModInfo parseModInfo(Path modInfo, Path baseDirectory) {
        if (!Files.isRegularFile(modInfo)) {
            return null;
        }

        String id = null;
        String name = null;
        String author = null;
        List<JarEntry> jarEntries = new ArrayList<>();

        try (BufferedReader reader = Files.newBufferedReader(modInfo, StandardCharsets.UTF_8)) {
            String line;
            boolean firstLine = true;

            while ((line = reader.readLine()) != null) {
                if (firstLine) {
                    line = stripBom(line);
                    firstLine = false;
                }

                int separator = line.indexOf('=');
                if (separator < 0) {
                    continue;
                }

                String key = line.substring(0, separator).trim();
                String value = unquote(line.substring(separator + 1).trim());

                if (key.equalsIgnoreCase("id")) {
                    if (id == null || id.isBlank()) {
                        id = value;
                    }
                } else if (key.equalsIgnoreCase("name")) {
                    if (name == null || name.isBlank()) {
                        name = value;
                    }
                } else if (key.equalsIgnoreCase("author") || key.equalsIgnoreCase("authors")) {
                    if (author == null || author.isBlank()) {
                        author = value;
                    }
                } else if (key.equalsIgnoreCase(ZomboidConstants.JAR_PLUGINS_KEY)) {
                    for (String entry : value.split(JAR_SEPARATOR_PATTERN)) {
                        String jarPath = entry.trim();
                        if (!jarPath.isEmpty()) {
                            jarEntries.add(new JarEntry(jarPath, baseDirectory));
                        }
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.debug("Failed to read mod.info [{}]", modInfo, e);
            return null;
        }

        return new ModInfo(id, name, author, List.copyOf(jarEntries));
    }

    private static int addJarCandidates(
            ModInfo info,
            Path fallbackDirectory,
            Path realModRoot,
            String modId,
            String modName,
            String modAuthor,
            String workshopId,
            Map<Path, PluginArchive> archives,
            Set<String> errors
    ) {
        int before = archives.size();

        for (JarEntry entry : info.jarEntries()) {
            Path resolved = resolveJar(entry, fallbackDirectory, realModRoot, modId, errors);
            if (resolved != null) {
                Map<String, String> attributes = new LinkedHashMap<>();
                if (workshopId != null) {
                    attributes.put(PluginArchive.ATTRIBUTE_WORKSHOP_ID, workshopId);
                }
                if (!modAuthor.isBlank()) {
                    attributes.put(PluginArchive.ATTRIBUTE_AUTHOR, modAuthor);
                }
                archives.putIfAbsent(
                        resolved,
                        new PluginArchive(
                                resolved.toFile(),
                                modId,
                                modName,
                                workshopId != null ? PluginArchive.SOURCE_WORKSHOP : PluginArchive.SOURCE_MOD,
                                attributes,
                                false
                        )
                );
            }
        }

        return archives.size() - before;
    }

    private static Path resolveJar(
            JarEntry entry,
            Path fallbackDirectory,
            Path realModRoot,
            String modId,
            Set<String> errors
    ) {
        Path relative;
        try {
            relative = Path.of(entry.value());
        } catch (RuntimeException e) {
            errors.add("Invalid jarPlugins path [" + entry.value() + "] in mod [" + modId + "]");
            return null;
        }

        if (relative.isAbsolute() || relative.getRoot() != null) {
            errors.add("Absolute or rooted jarPlugins path [" + entry.value() + "] is not allowed in mod [" + modId + "]");
            return null;
        }

        if (containsParentTraversal(relative)) {
            errors.add("jarPlugins path [" + entry.value() + "] must not contain '..' in mod [" + modId + "]");
            return null;
        }

        Path primary = resolveInsideMod(relative, entry.baseDirectory(), realModRoot, errors);
        if (primary != null) {
            LOGGER.debug("Resolved plugin archive [{}] from mod [{}] via [{}]", primary, modId, entry.baseDirectory());
            return primary;
        }

        Path fallback = resolveInsideMod(relative, fallbackDirectory, realModRoot, errors);
        if (fallback != null) {
            LOGGER.debug("Resolved plugin archive [{}] from mod [{}] via [{}]", fallback, modId, fallbackDirectory);
            return fallback;
        }

        String message = "Plugin archive [" + entry.value() + "] listed in [" + entry.baseDirectory()
                + "] for mod [" + modId + "] was not found";
        errors.add(message);
        LOGGER.warn(message);
        return null;
    }

    private static boolean containsParentTraversal(Path path) {
        for (Path part : path) {
            if ("..".equals(part.toString())) {
                return true;
            }
        }
        return false;
    }

    private static Path resolveInsideMod(
            Path relative,
            Path baseDirectory,
            Path realModRoot,
            Set<String> errors
    ) {
        if (baseDirectory == null) {
            return null;
        }

        Path candidate = baseDirectory.resolve(relative).toAbsolutePath().normalize();
        if (!Files.isRegularFile(candidate)) {
            return null;
        }

        try {
            Path realCandidate = candidate.toRealPath();
            if (!realCandidate.startsWith(realModRoot)) {
                String message = "Rejected jarPlugins path outside mod directory: [" + relative
                        + "] in [" + baseDirectory + "]";
                errors.add(message);
                LOGGER.warn(message);
                return null;
            }
            return realCandidate;
        } catch (IOException e) {
            LOGGER.debug("Failed to resolve plugin archive [{}]", candidate, e);
            return null;
        }
    }

    private Path findSteamApps() {
        Path current = gameRoot;
        while (current != null) {
            Path name = current.getFileName();
            if (name != null && name.toString().equalsIgnoreCase("steamapps")) {
                return current;
            }
            current = current.getParent();
        }
        return null;
    }

    private static String workshopItemId(Path modDirectory) {
        String normalized = modDirectory.toString().replace('\\', '/') + "/";
        Matcher matcher = WORKSHOP_ITEM_PATTERN.matcher(normalized);
        return matcher.find() ? matcher.group(1) : null;
    }

    private static Version parseVersion(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String normalized = value.trim();
        if (normalized.matches("\\d+")) {
            normalized += ".0.0";
        } else if (normalized.matches("\\d+\\.\\d+")) {
            normalized += ".0";
        }

        try {
            return Version.parse(normalized);
        } catch (Exception e) {
            return null;
        }
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private static String stripTrailingComma(String value) {
        int comma = value.indexOf(',');
        if (comma >= 0) {
            value = value.substring(0, comma);
        }
        return value.trim();
    }

    private static String unquote(String value) {
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    private static String stripBom(String value) {
        if (!value.isEmpty() && value.charAt(0) == '\uFEFF') {
            return value.substring(1);
        }
        return value;
    }

    private static boolean startsWithIgnoreCase(String value, String prefix) {
        return value.regionMatches(true, 0, prefix, 0, prefix.length());
    }

    private record VersionedDirectory(Path directory, Version version) {
    }

    private record ModInfo(String id, String name, String author, List<JarEntry> jarEntries) {
    }

    private record JarEntry(String value, Path baseDirectory) {
    }
}
