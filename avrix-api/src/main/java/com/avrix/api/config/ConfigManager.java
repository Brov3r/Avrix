package com.avrix.api.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.ConfigurationOptions;
import org.spongepowered.configurate.yaml.NodeStyle;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Utility class for managing YAML configuration files using Configurate.
 */
public final class ConfigManager {
    private static final Logger log = LoggerFactory.getLogger(ConfigManager.class);

    /**
     * Default configuration filename used when no explicit path is provided.
     */
    private static final String DEFAULT_CONFIG_NAME = "config.yml";

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private ConfigManager() {
        // Utility class: prevent instantiation
    }

    /**
     * Creates a new empty configuration node with default options and BLOCK style.
     * <p>
     * The returned node is suitable for programmatic configuration building.
     * Use {@link #save(ConfigurationNode, Path)} to persist changes to disk.
     *
     * @return a new {@link ConfigurationNode} with default options
     */
    public static ConfigurationNode create() {
        return create(ConfigurationOptions.defaults(), NodeStyle.BLOCK);
    }

    /**
     * Creates a new empty configuration node with specified options and YAML style.
     * <p>
     * <strong>Note:</strong> {@code NodeStyle} affects only YAML serialization formatting
     * during {@link #save(ConfigurationNode, Path, NodeStyle)} operations. It is not
     * stored in the node itself and does not affect in-memory structure or loading.
     *
     * @param options the configuration options to apply (e.g., default value handling)
     * @param style   the YAML output style: {@link NodeStyle#BLOCK} for readable multiline,
     *                {@link NodeStyle#FLOW} for compact inline format
     * @return a new {@link ConfigurationNode} configured with the given options
     */
    public static ConfigurationNode create(ConfigurationOptions options, NodeStyle style) {
        return YamlConfigurationLoader.builder().nodeStyle(style).build().createNode(options);
    }

    /**
     * Loads a configuration node from the specified filesystem path.
     * <p>
     * The file must exist and contain valid YAML. Parent directories are not created
     * automatically — ensure the path is writable before calling this method.
     *
     * @param path the path to the YAML configuration file
     * @return the loaded {@link ConfigurationNode} with parsed content
     * @throws ConfigurateException if the file does not exist or contains malformed YAML
     * @throws NullPointerException if path is null
     */
    public static ConfigurationNode load(Path path) throws ConfigurateException {
        Objects.requireNonNull(path, "Path cannot be null");

        if (!Files.exists(path)) {
            throw new ConfigurateException("Configuration file not found: " + path);
        }

        return YamlConfigurationLoader.builder()
                .path(path)
                .build()
                .load();
    }

    /**
     * Loads a configuration from JAR archive with filesystem fallback and auto-extraction.
     * <p>
     * Processing order:
     * <ol>
     *   <li>Validate JAR file and internal path arguments</li>
     *   <li>Resolve work directory: {@code <jar-dir>/<jar-name>/}</li>
     *   <li>Attempt to load from filesystem if config already extracted</li>
     *   <li>Otherwise, extract from JAR resource to work directory and load</li>
     * </ol>
     * <p>
     * <strong>Security:</strong> Path traversal attempts in {@code internalPath} are
     * neutralized by extracting only the filename component. The work directory is
     * isolated adjacent to the JAR to prevent unauthorized file access.
     *
     * @param jarFile      the JAR archive to search for embedded configuration
     * @param internalPath relative path inside JAR (e.g., {@code "config.yml"} or
     *                     {@code "modules/auth/config.yml"}), or blank for default name
     * @return the loaded {@link ConfigurationNode}
     * @throws IOException              if file operations, JAR access, or directory creation fails
     * @throws ConfigurateException     if YAML parsing fails
     * @throws FileNotFoundException    if JAR does not exist or config entry not found
     * @throws IllegalArgumentException if internalPath has invalid extension
     * @throws NullPointerException     if jarFile or internalPath is null
     */
    public static ConfigurationNode load(File jarFile, String internalPath) throws IOException {
        Objects.requireNonNull(jarFile, "jarFile cannot be null");
        Objects.requireNonNull(internalPath, "internalPath cannot be null");

        if (!jarFile.exists()) {
            throw new FileNotFoundException("JAR file must exist and be accessible: " + jarFile);
        }

        String configFileName = internalPath.isBlank() ? DEFAULT_CONFIG_NAME : internalPath;

        if (!configFileName.endsWith(".yml") && !configFileName.endsWith(".yaml")) {
            throw new IllegalArgumentException("Config path must have .yml or .yaml extension: " + configFileName);
        }

        String normalizedPath = configFileName.replace('\\', '/');
        if (normalizedPath.startsWith("/")) {
            normalizedPath = normalizedPath.substring(1);
        }

        Path workDir = getWorkDir(jarFile);
        Path targetPath = workDir.resolve(normalizedPath).normalize();

        Path normalizedWorkDir = workDir.normalize();
        if (!targetPath.startsWith(normalizedWorkDir)) {
            throw new SecurityException("Path traversal attempt detected: " + configFileName);
        }

        if (Files.exists(targetPath)) {
            log.trace("Loading configuration from filesystem: {}", targetPath);
            return load(targetPath);
        }

        try (JarFile jar = new JarFile(jarFile)) {
            JarEntry entry = findJarEntry(jar, configFileName);

            if (entry != null) {
                log.trace("Found configuration in JAR: {}, extracting to: {}", entry.getName(), targetPath);
                try (InputStream is = jar.getInputStream(entry)) {
                    Path parent = targetPath.getParent();
                    if (parent != null) {
                        Files.createDirectories(parent);
                    }
                    Files.copy(is, targetPath, StandardCopyOption.REPLACE_EXISTING);
                }
                return load(targetPath);
            } else {
                throw new FileNotFoundException("Configuration '" + configFileName + "' not found in JAR: " + jarFile.getName());
            }
        }
    }

    /**
     * Loads the default configuration file ({@value #DEFAULT_CONFIG_NAME} - 'config.yml') from JAR.
     * <p>
     * Convenience wrapper for {@link #load(File, String)} with the default config name.
     *
     * @param jarFile the JAR archive containing the default configuration
     * @return the loaded {@link ConfigurationNode}
     * @throws IOException           if file operations or JAR access fails
     * @throws ConfigurateException  if YAML parsing fails
     * @throws FileNotFoundException if JAR or default config entry not found
     */
    public static ConfigurationNode loadDefault(File jarFile) throws IOException {
        return load(jarFile, DEFAULT_CONFIG_NAME);
    }

    /**
     * Loads an existing configuration from JAR work directory or creates a new empty one.
     * <p>
     * Uses default options and BLOCK style for new nodes. See
     * {@link #loadOrCreate(File, Path, ConfigurationOptions, NodeStyle)} for details.
     *
     * @param jarFile the JAR archive to resolve work directory from
     * @param path    relative path for the configuration file within work directory
     * @return loaded or newly created {@link ConfigurationNode}
     * @throws IOException           if file operations or directory creation fails
     * @throws ConfigurateException  if existing file contains malformed YAML
     * @throws FileNotFoundException if JAR does not exist
     * @throws NullPointerException  if jarFile or path is null
     */
    public static ConfigurationNode loadOrCreate(File jarFile, Path path) throws IOException {
        Objects.requireNonNull(jarFile, "jarFile cannot be null");
        Objects.requireNonNull(path, "internalPath cannot be null");

        if (!jarFile.exists()) {
            throw new FileNotFoundException("JAR file must exist and be accessible: " + jarFile);
        }

        return loadOrCreate(getWorkDir(jarFile).resolve(path), ConfigurationOptions.defaults(), NodeStyle.BLOCK);
    }

    /**
     * Loads an existing configuration from JAR work directory or creates a new one.
     * <p>
     * Delegates to {@link #loadOrCreate(Path, ConfigurationOptions, NodeStyle)} after
     * resolving the absolute path within the JAR's work directory.
     *
     * @param jarFile the JAR archive to resolve work directory from
     * @param path    relative path for the configuration file within work directory
     * @param options configuration options for new node creation
     * @param style   YAML output style for future save operations
     * @return loaded or newly created {@link ConfigurationNode}
     * @throws IOException           if file operations or directory creation fails
     * @throws ConfigurateException  if existing file contains malformed YAML
     * @throws FileNotFoundException if JAR does not exist
     * @throws NullPointerException  if any argument is null
     */
    public static ConfigurationNode loadOrCreate(File jarFile, Path path, ConfigurationOptions options, NodeStyle style)
            throws IOException {
        return loadOrCreate(getWorkDir(jarFile).resolve(path), options, style);
    }

    /**
     * Loads an existing configuration file or creates a new empty one if missing.
     * <p>
     * Uses default options and BLOCK style. Parent directories are created automatically
     * if the file does not exist. The returned node is not automatically saved — call
     * {@link #save(ConfigurationNode, Path)} to persist changes.
     *
     * @param path the path to the configuration file
     * @return loaded {@link ConfigurationNode} or new empty node
     * @throws IOException          if directory creation fails or file access error occurs
     * @throws ConfigurateException if existing file contains malformed YAML
     * @throws NullPointerException if path is null
     */
    public static ConfigurationNode loadOrCreate(Path path)
            throws IOException {
        return loadOrCreate(path, ConfigurationOptions.defaults(), NodeStyle.BLOCK);
    }

    /**
     * Loads an existing configuration file or creates a new one with specified options.
     * <p>
     * Behavior:
     * <ul>
     *   <li>If file exists: loads and returns populated node via {@link #load(Path)}</li>
     *   <li>If file missing: creates new empty node via {@link #create(ConfigurationOptions, NodeStyle)}</li>
     * </ul>
     * <p>
     * Parent directories are created automatically if missing. The {@code style} parameter
     * affects only future {@code save()} operations, not the in-memory node structure.
     *
     * @param path    the path to the configuration file
     * @param options configuration options for new node creation
     * @param style   YAML output style for future save operations
     * @return loaded or newly created {@link ConfigurationNode}
     * @throws IOException          if directory creation fails or file access error occurs
     * @throws ConfigurateException if existing file contains malformed YAML
     * @throws NullPointerException if any argument is null
     */
    public static ConfigurationNode loadOrCreate(Path path, ConfigurationOptions options, NodeStyle style)
            throws IOException {
        Objects.requireNonNull(path, "Path cannot be null");
        Objects.requireNonNull(options, "Options cannot be null");
        Objects.requireNonNull(style, "NodeStyle cannot be null");

        if (Files.exists(path)) {
            log.trace("Configuration exists, loading from: {}", path);
            return load(path);
        }

        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
            log.trace("Created parent directories for new config: {}", parent);
        }


        log.trace("Configuration not found, creating new node for: {}", path);
        ConfigurationNode node = create(options, style);

        save(node, path, style);

        return node;
    }

    /**
     * Saves a configuration node to the specified filesystem path in YAML format.
     * <p>
     * Uses BLOCK style for human-readable multiline output. Parent directories are
     * created automatically if missing. Existing files are overwritten.
     *
     * @param config the {@link ConfigurationNode} to save
     * @param path   the target filesystem path for the YAML file
     * @throws IOException          if directory creation fails or write operation fails
     * @throws NullPointerException if config or path is null
     */
    public static void save(ConfigurationNode config, Path path) throws IOException {
        save(config, path, NodeStyle.BLOCK);
    }

    /**
     * Saves a configuration node to the specified filesystem path with custom YAML style.
     * <p>
     * Parent directories are created automatically if missing. Existing files are
     * overwritten. The {@code style} parameter controls output formatting only.
     *
     * @param config the {@link ConfigurationNode} to save
     * @param path   the target filesystem path for the YAML file
     * @param style  YAML output style: {@link NodeStyle#BLOCK} for readable multiline,
     *               {@link NodeStyle#FLOW} for compact inline format
     * @throws IOException          if directory creation fails or write operation fails
     * @throws NullPointerException if config, path, or style is null
     */
    public static void save(ConfigurationNode config, Path path, NodeStyle style) throws IOException {
        Objects.requireNonNull(config, "ConfigurationNode must not be null");
        Objects.requireNonNull(path, "Path must not be null");

        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        YamlConfigurationLoader.builder()
                .path(path)
                .nodeStyle(style)
                .build()
                .save(config);

        log.debug("Configuration saved to: {}", path);
    }

    /**
     * Finds a JAR entry by name with flexible path matching strategies.
     * <p>
     * Search order:
     * <ol>
     *   <li>Exact path match (normalized with forward slashes)</li>
     *   <li>Path with/without leading slash variation</li>
     *   <li>Fallback: filename-only match for nested resources</li>
     * </ol>
     *
     * @param jar           the open {@link JarFile} to search
     * @param requestedPath the requested entry path (may contain ./ or be nested)
     * @return the matching {@link JarEntry}, or null if not found
     */
    private static JarEntry findJarEntry(JarFile jar, String requestedPath) {
        String normalized = requestedPath.replace('\\', '/');

        JarEntry entry = jar.getJarEntry(normalized);
        if (entry != null && !entry.isDirectory()) return entry;

        entry = jar.getJarEntry(normalized.startsWith("/") ? normalized.substring(1) : "/" + normalized);
        if (entry != null && !entry.isDirectory()) return entry;

        String targetFileName = getFileName(normalized);
        return jar.stream()
                .filter(e -> !e.isDirectory())
                .filter(e -> {
                    String name = e.getName();
                    return name.endsWith("/" + targetFileName) || name.equals(targetFileName);
                })
                .findFirst()
                .orElse(null);
    }

    /**
     * Resolves the work directory for a given JAR file.
     * <p>
     * Work directory is created adjacent to the JAR: {@code <jar-dir>/<jar-name>/}.
     * If the JAR has no parent directory (e.g., launched from current directory),
     * the work directory is created in the current working directory.
     *
     * @param jarFile the JAR archive to resolve work directory for
     * @return the absolute path to the work directory (created if missing)
     * @throws IOException if directory creation fails
     */
    /**
     * Resolves the work directory for a given JAR file.
     * <p>
     * Work directory is created adjacent to the JAR: {@code <jar-dir>/<sanitized-jar-name>/}.
     * The JAR name is sanitized to contain only alphanumeric characters, hyphens,
     * underscores, and dots to ensure filesystem compatibility across platforms.
     * <p>
     * If the JAR has no parent directory (e.g., launched from current directory),
     * the work directory is created in the current working directory.
     *
     * @param jarFile the JAR archive to resolve work directory for
     * @return the absolute path to the sanitized work directory (created if missing)
     * @throws IOException if directory creation fails
     */
    public static Path getWorkDir(File jarFile) throws IOException {
        Path jarPath = jarFile.toPath().toAbsolutePath().normalize();
        Path jarDir = jarPath.getParent();

        if (jarDir == null) {
            jarDir = Path.of("").toAbsolutePath();
        }

        String jarBaseName = jarFile.getName();
        if (jarBaseName.endsWith(".jar")) {
            jarBaseName = jarBaseName.substring(0, jarBaseName.length() - 4);
        }

        String sanitizedName = sanitizeDirectoryName(jarBaseName);

        Path workDir = jarDir.resolve(sanitizedName);
        Files.createDirectories(workDir);
        return workDir;
    }

    /**
     * Sanitizes a string to be safe for use as a directory name.
     * <p>
     * Rules:
     * <ul>
     *   <li>Keep: ASCII letters (a-z, A-Z), digits (0-9), hyphen (-), underscore (_), dot (.)</li>
     *   <li>Replace: all other characters with underscore (_)</li>
     *   <li>Collapse: multiple consecutive underscores into one</li>
     *   <li>Trim: leading/trailing underscores and dots</li>
     *   <li>Fallback: if result is empty, use "config"</li>
     * </ul>
     *
     * @param name the original directory name (e.g., JAR filename without extension)
     * @return sanitized name safe for filesystem use
     */
    private static String sanitizeDirectoryName(String name) {
        if (name == null || name.isEmpty()) {
            return "config";
        }

        String sanitized = name.replaceAll("[^a-zA-Z0-9\\-_.]", "_");

        sanitized = sanitized.replaceAll("_+", "_");

        sanitized = sanitized.replaceAll("^[_.]+|[_.]+$", "");

        return sanitized.isEmpty() ? "WorkDir-" + name.hashCode() : sanitized;
    }

    /**
     * Extracts the filename component from a path string.
     * <p>
     * Handles both forward and backward slashes as separators. Used to neutralize
     * path traversal attempts by discarding directory components.
     *
     * @param path the input path string
     * @return the filename portion (after the last slash), or the original string
     * if no slash is present
     */
    private static String getFileName(String path) {
        int lastSlashIndex = path.lastIndexOf('/');
        return lastSlashIndex == -1 ? path : path.substring(lastSlashIndex + 1);
    }
}