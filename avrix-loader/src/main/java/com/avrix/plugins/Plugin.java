package com.avrix.plugins;

import com.avrix.utils.Constants;
import com.avrix.utils.YamlFile;
import org.tinylog.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Base class for Avrix plugins.
 */
public abstract class Plugin {
    /**
     * Lazily initialized default configuration. Published safely via {@code volatile}.
     */
    private volatile YamlFile defaultConfig;

    /**
     * Immutable plugin metadata provided by the plugin loader.
     */
    private final Metadata metadata;

    /**
     * Cached plugin id for fast access and to validate it once at construction time.
     */
    private final String pluginId;

    /**
     * Creates a plugin instance.
     *
     * @param metadata plugin metadata; must not be {@code null}
     * @throws NullPointerException     if {@code metadata} is {@code null}
     * @throws IllegalArgumentException if {@code metadata.getId()} is blank
     */
    protected Plugin(Metadata metadata) {
        this.metadata = Objects.requireNonNull(metadata, "metadata must not be null");
        this.pluginId = requireNonBlank(metadata.getId(), "metadata.id");
    }

    /**
     * Returns the default plugin config.
     * <p>
     * If the default config has not been loaded yet, it is loaded lazily.
     * </p>
     *
     * @return loaded default config; never {@code null}
     */
    public final YamlFile getDefaultConfig() {
        YamlFile cfg = defaultConfig;
        if (cfg == null) {
            loadDefaultConfig();
            cfg = defaultConfig;
        }
        return cfg;
    }

    /**
     * Returns the plugin config folder.
     * <p>
     * The default location is {@code <pluginsFolder>/<pluginId>}.
     * </p>
     *
     * @return config folder; never {@code null}
     */
    public final File getConfigFolder() {
        return new File(Constants.PLUGINS_FOLDER_NAME, pluginId);
    }

    /**
     * Returns plugin metadata.
     *
     * @return metadata; never {@code null}
     */
    public final Metadata getMetadata() {
        return metadata;
    }

    /**
     * Loads default configuration file into plugin config folder and stores it as {@link #getDefaultConfig()}.
     * <p>
     * If the config file does not exist, it will be copied from the plugin JAR if an entry with the same name exists.
     * Otherwise, an empty file will be created.
     * </p>
     */
    public final synchronized void loadDefaultConfig() {
        Path configFolder = ensureConfigFolderExists();
        Path target = configFolder.resolve(Constants.PLUGINS_DEFAULT_CONFIG_NAME);

        try {
            ensureFileExistsFromJarOrCreateEmpty(Constants.PLUGINS_DEFAULT_CONFIG_NAME, target);
            defaultConfig = new YamlFile(target.toFile());
        } catch (Exception e) {
            Logger.warn(e, "Failed to load default config from '{}', creating a new YAML file.", target);
            defaultConfig = YamlFile.create(target.toFile());
        }
    }

    /**
     * Copies a file from the plugin JAR into destination if the entry exists.
     * <p>
     * If the entry does not exist, an empty destination file will be created
     * (directories will be created as needed).
     * </p>
     *
     * @param fileName    file name inside JAR (e.g. {@code "config.yml"}); must not be blank
     * @param destination destination file; must not be {@code null}
     * @throws IllegalArgumentException if {@code fileName} is blank
     * @throws NullPointerException     if {@code destination} is {@code null}
     */
    public void copyConfigFromJar(String fileName, File destination) {
        Objects.requireNonNull(destination, "destination must not be null");
        String name = requireNonBlank(fileName, "fileName");

        Path path = destination.toPath();
        try {
            createParentDirectories(path);
        } catch (IOException e) {
            Logger.error(e, "Failed to create parent directories for '{}'.", path);
            return;
        }

        File jarFile = metadata.getPluginFile();
        if (jarFile == null || !jarFile.isFile()) {
            Logger.warn("Plugin JAR file is not available. Creating empty config '{}'.", path.getFileName());
            createEmptyFileIfMissing(path);
            return;
        }

        try (JarFile jar = new JarFile(jarFile)) {
            JarEntry entry = jar.getJarEntry(name);
            if (entry == null) {
                Logger.debug("Config template '{}' not found in plugin JAR. Creating empty file '{}'.", name, path.getFileName());
                createEmptyFileIfMissing(path);
                return;
            }

            try (InputStream in = jar.getInputStream(entry)) {
                Files.copy(in, path, StandardCopyOption.REPLACE_EXISTING);
                Logger.debug("Copied config '{}' from plugin JAR to '{}'.", name, path);
            }
        } catch (IOException e) {
            Logger.error(e, "Failed to copy config '{}' from plugin JAR to '{}'.", name, path);
            createEmptyFileIfMissing(path);
        }
    }

    /**
     * Loads a YAML config from the plugin config folder.
     * <p>
     * If the file does not exist, it will be copied from the plugin JAR if possible; otherwise, it will be created empty.
     * The provided path is normalized:
     * <ul>
     *   <li>trimmed</li>
     *   <li>ensures {@code .yml} extension</li>
     *   <li>rejects absolute paths</li>
     *   <li>rejects path traversal via {@code ..}</li>
     * </ul>
     * </p>
     *
     * @param configPath relative path within config folder (e.g. {@code "my-config.yml"} or {@code "sub/other"})
     * @return loaded YAML file (existing or newly created)
     * @throws IllegalArgumentException if {@code configPath} is blank, absolute, or attempts path traversal
     */
    public final synchronized YamlFile loadConfig(String configPath) {
        String normalized = normalizeConfigPath(configPath);

        Path configFolder = ensureConfigFolderExists();
        Path target = configFolder.resolve(normalized).normalize();

        if (!target.startsWith(configFolder.normalize())) {
            throw new IllegalArgumentException("configPath must be within the config folder");
        }

        try {
            ensureFileExistsFromJarOrCreateEmpty(normalized, target);
            return new YamlFile(target.toFile());
        } catch (Exception e) {
            Logger.warn(e, "Failed to load config '{}', creating a new YAML file.", target);
            return YamlFile.create(target.toFile());
        }
    }

    /**
     * Called during plugin initialization phase.
     */
    public abstract void onInitialize();

    /**
     * Called when the plugin is launched (runtime start).
     */
    public abstract void onLaunch();

    /**
     * Ensures the plugin config folder exists and returns it as a {@link Path}.
     *
     * @return existing or newly created config folder path
     * @throws IllegalStateException if the folder cannot be created
     */
    private Path ensureConfigFolderExists() {
        Path path = getConfigFolder().toPath();
        try {
            Files.createDirectories(path);
            return path;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to create plugin config folder: " + path, e);
        }
    }

    /**
     * Normalizes a config path:
     * <ul>
     *   <li>Trims whitespace</li>
     *   <li>Converts backslashes to forward slashes</li>
     *   <li>Ensures {@code .yml} extension</li>
     *   <li>Rejects absolute paths and {@code ..} traversal</li>
     * </ul>
     *
     * @param configPath raw config path
     * @return normalized relative config path
     * @throws IllegalArgumentException if the path is invalid or unsafe
     */
    private static String normalizeConfigPath(String configPath) {
        String trimmed = requireNonBlank(configPath, "configPath").trim().replace('\\', '/');
        if (!trimmed.endsWith(".yml")) {
            trimmed = trimmed + ".yml";
        }

        Path rel = Path.of(trimmed).normalize();
        if (rel.isAbsolute() || rel.startsWith("..")) {
            throw new IllegalArgumentException("configPath must be a safe relative path within the config folder");
        }

        return rel.toString().replace('\\', '/');
    }

    /**
     * Ensures the given target file exists:
     * <ul>
     *   <li>Creates parent directories if needed</li>
     *   <li>If the file does not exist, attempts to copy it from the plugin JAR</li>
     *   <li>If copying is not possible, creates an empty file</li>
     * </ul>
     *
     * @param jarEntryName name of the entry in the plugin JAR
     * @param target       destination file path
     * @throws IOException if parent directory creation fails
     */
    private void ensureFileExistsFromJarOrCreateEmpty(String jarEntryName, Path target) throws IOException {
        createParentDirectories(target);

        if (Files.exists(target)) {
            return;
        }

        copyConfigFromJar(jarEntryName, target.toFile());
        if (!Files.exists(target)) {
            createEmptyFileIfMissing(target);
        }
    }

    /**
     * Creates parent directories for the given file path if they do not exist.
     *
     * @param file file path
     * @throws IOException if directory creation fails
     */
    private static void createParentDirectories(Path file) throws IOException {
        Path parent = file.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
    }

    /**
     * Creates an empty file if it does not exist.
     * <p>
     * Parent directories are created if needed.
     * </p>
     *
     * @param file file path
     */
    private static void createEmptyFileIfMissing(Path file) {
        try {
            if (Files.notExists(file)) {
                createParentDirectories(file);
                Files.createFile(file);
            }
        } catch (IOException e) {
            Logger.error(e, "Failed to create empty file '{}'.", file);
        }
    }

    /**
     * Validates that a string is non-null and not blank.
     *
     * @param value value to validate
     * @param field field name for error messages
     * @return original value
     * @throws NullPointerException     if {@code value} is {@code null}
     * @throws IllegalArgumentException if {@code value} is blank
     */
    private static String requireNonBlank(String value, String field) {
        Objects.requireNonNull(value, field + " must not be null");
        if (value.trim().isEmpty()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }
}
