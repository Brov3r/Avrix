package com.avrix.utils;

import org.tinylog.Logger;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Thread-safe YAML document wrapper with dot-path access.
 *
 * <p>
 * The class supports loading YAML from:
 * </p>
 * <ul>
 *   <li>filesystem {@link Path}/{@link File}/{@link String}</li>
 *   <li>{@link URL}/{@link URI}</li>
 *   <li>an embedded YAML resource inside a JAR file</li>
 *   <li>an {@link InputStream}</li>
 * </ul>
 *
 * <p>
 * YAML is deserialized into a {@code Map<String, Object>} root. Nested maps can be accessed
 * via dot-notation keys (e.g. {@code "plugin.id"}).
 * </p>
 *
 * <p>
 * All reads and writes are synchronized; returned collections are defensive copies to prevent
 * external mutation.
 * </p>
 */
public final class YamlFile {

    private static final Yaml DEFAULT_YAML = new Yaml();

    private final Object lock = new Object();

    private final Path filePath;
    private final String fileName;

    /**
     * Root YAML data. The reference is swapped only under {@link #lock}.
     */
    private Map<String, Object> yamlData;

    /**
     * Loads a YAML file embedded inside a JAR (e.g. {@code plugin.yml}).
     *
     * <p>
     * This constructor reads the YAML entry using {@link JarFile} to avoid issues
     * with {@code jar:file:} URL encoding (especially on Windows).
     * </p>
     *
     * @param jarFilePath      absolute or relative path to the JAR file; must not be {@code null} or blank
     * @param internalYamlPath path to the YAML inside the JAR; must not be {@code null} or blank
     * @throws NullPointerException     if any argument is {@code null}
     * @throws IllegalArgumentException if any argument is blank
     * @throws IllegalStateException    if the JAR cannot be read or the entry cannot be parsed
     */
    public YamlFile(String jarFilePath, String internalYamlPath) {
        Objects.requireNonNull(jarFilePath, "Jar file path must not be null");
        Objects.requireNonNull(internalYamlPath, "Internal YAML path must not be null");

        String jarPath = jarFilePath.trim();
        String entryPath = internalYamlPath.trim();
        if (jarPath.isEmpty()) {
            throw new IllegalArgumentException("Jar file path must not be blank.");
        }
        if (entryPath.isEmpty()) {
            throw new IllegalArgumentException("Internal YAML path must not be blank.");
        }

        Path path = Paths.get(jarPath);
        this.filePath = path.toAbsolutePath().normalize().resolve(entryPath);
        this.fileName = extractFileName(entryPath);

        this.yamlData = new LinkedHashMap<>();

        loadFromJar(path, entryPath);
    }

    /**
     * Loads a YAML file from an {@link InputStream}.
     *
     * <p>
     * The stream is always closed by this constructor.
     * </p>
     *
     * @param yamlInputStream stream containing YAML; must not be {@code null}
     * @param sourcePath      path associated with this YAML (for diagnostics); may be {@code null}
     * @param sourceName      file name associated with this YAML (for diagnostics); must not be {@code null} or blank
     * @throws NullPointerException     if {@code yamlInputStream} or {@code sourceName} is {@code null}
     * @throws IllegalArgumentException if {@code sourceName} is blank
     * @throws IllegalStateException    if the YAML cannot be parsed
     */
    public YamlFile(InputStream yamlInputStream, Path sourcePath, String sourceName) {
        Objects.requireNonNull(yamlInputStream, "YAML input stream must not be null");
        Objects.requireNonNull(sourceName, "Source name must not be null");

        String name = sourceName.trim();
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Source name must not be blank.");
        }

        this.filePath = (sourcePath == null) ? Paths.get(name) : sourcePath.toAbsolutePath().normalize();
        this.fileName = name;
        this.yamlData = new LinkedHashMap<>();

        loadFromStream(yamlInputStream, "InputStream(" + name + ")");
    }

    /**
     * Loads a YAML file from a {@link Path}.
     *
     * @param yamlPath path to YAML; must not be {@code null}
     * @throws IOException           if an I/O error occurs while opening the file
     * @throws IllegalStateException if YAML cannot be parsed
     */
    public YamlFile(Path yamlPath) throws IOException {
        Objects.requireNonNull(yamlPath, "YAML path must not be null");
        Path normalized = yamlPath.toAbsolutePath().normalize();

        this.filePath = normalized;
        this.fileName = normalized.getFileName().toString();
        this.yamlData = new LinkedHashMap<>();

        try (InputStream in = Files.newInputStream(normalized)) {
            loadFromStream(in, normalized.toString());
        }
    }

    /**
     * Loads a YAML file from a {@link URL}.
     *
     * @param yamlUrl URL pointing to YAML; must not be {@code null}
     * @throws IOException           if an I/O error occurs
     * @throws URISyntaxException    if URL cannot be converted to a {@link Path}
     * @throws IllegalStateException if YAML cannot be parsed
     */
    public YamlFile(URL yamlUrl) throws IOException, URISyntaxException {
        Objects.requireNonNull(yamlUrl, "YAML URL must not be null");

        Path path = Paths.get(yamlUrl.toURI()).toAbsolutePath().normalize();
        this.filePath = path;
        this.fileName = path.getFileName().toString();
        this.yamlData = new LinkedHashMap<>();

        try (InputStream in = yamlUrl.openStream()) {
            loadFromStream(in, yamlUrl.toExternalForm());
        }
    }

    /**
     * Loads a YAML file from a {@link URI}.
     *
     * @param yamlUri URI pointing to YAML; must not be {@code null}
     * @throws IOException           if an I/O error occurs
     * @throws IllegalStateException if YAML cannot be parsed
     */
    public YamlFile(URI yamlUri) throws IOException {
        Objects.requireNonNull(yamlUri, "YAML URI must not be null");

        Path path = Paths.get(yamlUri).toAbsolutePath().normalize();
        this.filePath = path;
        this.fileName = path.getFileName().toString();
        this.yamlData = new LinkedHashMap<>();

        try (InputStream in = yamlUri.toURL().openStream()) {
            loadFromStream(in, yamlUri.toString());
        }
    }

    /**
     * Loads a YAML file from a {@link File}.
     *
     * @param yamlFile YAML file; must not be {@code null}
     * @throws IOException if an I/O error occurs
     */
    public YamlFile(File yamlFile) throws IOException {
        this(Objects.requireNonNull(yamlFile, "YAML file must not be null").toURI());
    }

    /**
     * Loads a YAML file from a filesystem path string.
     *
     * @param yamlFilePath YAML file path string; must not be {@code null}
     * @throws IOException if an I/O error occurs
     */
    public YamlFile(String yamlFilePath) throws IOException {
        this(new File(Objects.requireNonNull(yamlFilePath, "YAML file path must not be null")));
    }

    /**
     * Returns the resolved file path associated with this YAML document.
     *
     * @return resolved path, never {@code null}
     */
    public Path getFilePath() {
        return filePath;
    }

    /**
     * Returns the display file name associated with this YAML document.
     *
     * @return file name, never {@code null}
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * Saves the YAML document to the specified path using UTF-8 encoding.
     *
     * @param targetPath path string to save to; must not be {@code null} or blank
     * @throws IllegalArgumentException if {@code targetPath} is blank
     * @throws IllegalStateException    if saving fails
     */
    public void save(String targetPath) {
        Objects.requireNonNull(targetPath, "Target path must not be null");
        String path = targetPath.trim();
        if (path.isEmpty()) {
            throw new IllegalArgumentException("Target path must not be blank.");
        }

        Path out = Paths.get(path).toAbsolutePath().normalize();
        save(out);
    }

    /**
     * Saves the YAML document to its original {@link #getFilePath()}.
     *
     * @throws IllegalStateException if saving fails
     */
    public void save() {
        save(filePath);
    }

    /**
     * Loads a YAML file from disk if it exists.
     *
     * <p>
     * This method preserves the original API that returns {@code null} on failure.
     * Prefer {@link #loadStrict(Path)} when possible.
     * </p>
     *
     * @param yamlFile YAML file; must not be {@code null}
     * @return loaded {@link YamlFile} instance or {@code null} if missing or unreadable
     */
    public static YamlFile load(File yamlFile) {
        Objects.requireNonNull(yamlFile, "YAML file must not be null");
        if (!yamlFile.exists()) {
            Logger.warn("YAML file '{}' does not exist.", yamlFile.getAbsolutePath());
            return null;
        }
        try {
            return new YamlFile(yamlFile);
        } catch (Exception e) {
            Logger.error("Failed to load YAML file '{}'.", yamlFile.getAbsolutePath(), e);
            return null;
        }
    }

    /**
     * Loads a YAML file from disk if it exists.
     *
     * @param yamlPath YAML path; must not be {@code null}
     * @return loaded {@link YamlFile} instance or {@code null} if missing or unreadable
     */
    public static YamlFile load(Path yamlPath) {
        Objects.requireNonNull(yamlPath, "YAML path must not be null");
        return load(yamlPath.toFile());
    }

    /**
     * Loads a YAML file from disk if it exists.
     *
     * @param yamlFilePath YAML file path string; must not be {@code null}
     * @return loaded {@link YamlFile} instance or {@code null} if missing or unreadable
     */
    public static YamlFile load(String yamlFilePath) {
        Objects.requireNonNull(yamlFilePath, "YAML file path must not be null");
        return load(new File(yamlFilePath));
    }

    /**
     * Loads a YAML file or throws if it cannot be loaded.
     *
     * @param yamlPath YAML file path
     * @return loaded {@link YamlFile}
     * @throws IllegalStateException if the file does not exist or cannot be read/parsed
     */
    public static YamlFile loadStrict(Path yamlPath) {
        Objects.requireNonNull(yamlPath, "YAML path must not be null");
        if (!Files.isRegularFile(yamlPath)) {
            throw new IllegalStateException("YAML file does not exist: " + yamlPath.toAbsolutePath().normalize());
        }
        try {
            return new YamlFile(yamlPath);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load YAML file: " + yamlPath.toAbsolutePath().normalize(), e);
        }
    }

    /**
     * Creates a new empty YAML file on disk.
     *
     * <p>
     * This method preserves the original API that returns {@code null} on failure.
     * Prefer creating the file yourself and using {@link #loadStrict(Path)} when possible.
     * </p>
     *
     * @param yamlFile file to create; must not be {@code null}
     * @return created {@link YamlFile} or {@code null} if already exists or cannot be created
     */
    public static YamlFile create(File yamlFile) {
        Objects.requireNonNull(yamlFile, "YAML file must not be null");
        try {
            if (yamlFile.exists()) {
                Logger.warn("YAML file '{}' already exists.", yamlFile.getAbsolutePath());
                return null;
            }
            File parent = yamlFile.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                Logger.error("Failed to create directories for '{}'.", yamlFile.getAbsolutePath());
                return null;
            }
            if (!yamlFile.createNewFile()) {
                Logger.error("Failed to create YAML file '{}'.", yamlFile.getAbsolutePath());
                return null;
            }
            return new YamlFile(yamlFile);
        } catch (Exception e) {
            Logger.error("Failed to create YAML file '{}'.", yamlFile.getAbsolutePath(), e);
            return null;
        }
    }

    /**
     * Creates a new empty YAML file on disk.
     *
     * @param yamlPath YAML path; must not be {@code null}
     * @return created {@link YamlFile} or {@code null} if already exists or cannot be created
     */
    public static YamlFile create(Path yamlPath) {
        Objects.requireNonNull(yamlPath, "YAML path must not be null");
        return create(yamlPath.toFile());
    }

    /**
     * Creates a new empty YAML file on disk.
     *
     * @param yamlFilePath YAML path string; must not be {@code null}
     * @return created {@link YamlFile} or {@code null} if already exists or cannot be created
     */
    public static YamlFile create(String yamlFilePath) {
        Objects.requireNonNull(yamlFilePath, "YAML file path must not be null");
        return create(new File(yamlFilePath));
    }

    /**
     * Removes a value by dot-path key. Missing keys are ignored.
     *
     * @param dotKey key in dot notation (e.g. {@code "a.b.c"}); must not be {@code null} or blank
     * @throws IllegalArgumentException if the key is invalid
     */
    @SuppressWarnings("unchecked")
    public void remove(String dotKey) {
        Objects.requireNonNull(dotKey, "dotKey must not be null");

        synchronized (lock) {

            String[] segments = validateAndSplitDotPath(dotKey);

            Map<String, Object> data = yamlData;
            for (int i = 0; i < segments.length - 1; i++) {
                Object next = data.get(segments[i]);
                if (!(next instanceof Map<?, ?>)) {
                    return;
                }
                data = (Map<String, Object>) next;
            }
            data.remove(segments[segments.length - 1]);
        }
    }

    /**
     * Sets a value by dot-path key, creating intermediate maps as needed.
     *
     * @param dotKey key in dot notation (e.g. {@code "a.b.c"}); must not be {@code null} or blank
     * @param value  value to set; may be {@code null}
     * @throws IllegalArgumentException if the key is invalid
     */
    @SuppressWarnings("unchecked")
    public void setValue(String dotKey, Object value) {
        Objects.requireNonNull(dotKey, "dotKey must not be null");

        synchronized (lock) {
            String[] segments = validateAndSplitDotPath(dotKey);


            Map<String, Object> data = yamlData;
            for (int i = 0; i < segments.length - 1; i++) {
                Object current = data.get(segments[i]);
                if (!(current instanceof Map<?, ?>)) {
                    current = new LinkedHashMap<String, Object>();
                    data.put(segments[i], current);
                }
                data = (Map<String, Object>) current;
            }
            data.put(segments[segments.length - 1], value);
        }
    }

    /**
     * Gets a raw value by dot-path key.
     *
     * @param dotKey key in dot notation (e.g. {@code "a.b.c"}); must not be {@code null} or blank
     * @return stored value or {@code null} if not found
     * @throws IllegalArgumentException if the key is invalid
     */
    @SuppressWarnings("unchecked")
    public Object getValue(String dotKey) {
        Objects.requireNonNull(dotKey, "dotKey must not be null");

        synchronized (lock) {
            String[] segments = validateAndSplitDotPath(dotKey);

            Map<String, Object> data = yamlData;
            for (int i = 0; i < segments.length - 1; i++) {
                Object next = data.get(segments[i]);
                if (!(next instanceof Map<?, ?>)) {
                    return null;
                }
                data = (Map<String, Object>) next;
            }
            return data.get(segments[segments.length - 1]);
        }
    }

    /**
     * Returns a defensive copy of the full YAML root map.
     *
     * @return copy of root YAML data, never {@code null}
     */
    public Map<String, Object> getAll() {
        synchronized (lock) {
            return deepCopyMap(yamlData);
        }
    }

    /**
     * Checks whether a dot-path key exists in the YAML (value is not {@code null}).
     *
     * @param dotKey key in dot notation
     * @return {@code true} if present and non-null
     */
    public boolean contains(String dotKey) {
        return getValue(dotKey) != null;
    }

    /**
     * Clears all YAML data.
     */
    public void clear() {
        synchronized (lock) {
            yamlData.clear();
        }
    }

    /**
     * Checks whether the YAML root is empty.
     *
     * @return {@code true} if root map is empty
     */
    public boolean isEmpty() {
        synchronized (lock) {
            return yamlData.isEmpty();
        }
    }

    /**
     * Merges the provided map into this YAML document recursively.
     *
     * <p>
     * Map values are merged when both sides are maps; otherwise the source value overwrites the target.
     * </p>
     *
     * @param sourceData source data; must not be {@code null}
     */
    public void merge(Map<String, Object> sourceData) {
        Objects.requireNonNull(sourceData, "Source data must not be null");
        synchronized (lock) {
            mergeRecursive(yamlData, sourceData);
        }
    }

    /**
     * Merges another {@link YamlFile} into this YAML document.
     *
     * @param otherYaml other YAML file; must not be {@code null}
     */
    public void merge(YamlFile otherYaml) {
        Objects.requireNonNull(otherYaml, "Other YAML file must not be null");
        merge(otherYaml.getAll());
    }

    /**
     * Returns an integer value or {@code 0} if missing or not numeric.
     *
     * @param dotKey key in dot notation
     * @return int value or {@code 0}
     */
    public int getInt(String dotKey) {
        Object value = getValue(dotKey);
        return (value instanceof Number n) ? n.intValue() : 0;
    }

    /**
     * Sets an integer value.
     *
     * @param dotKey   key in dot notation
     * @param intValue value to set
     */
    public void setInt(String dotKey, int intValue) {
        setValue(dotKey, intValue);
    }

    /**
     * Returns a long value or {@code 0L} if missing or not numeric.
     *
     * @param dotKey key in dot notation
     * @return long value or {@code 0L}
     */
    public long getLong(String dotKey) {
        Object value = getValue(dotKey);
        return (value instanceof Number n) ? n.longValue() : 0L;
    }

    /**
     * Sets a long value.
     *
     * @param dotKey    key in dot notation
     * @param longValue value to set
     */
    public void setLong(String dotKey, long longValue) {
        setValue(dotKey, longValue);
    }

    /**
     * Returns a short value or {@code 0} if missing or not numeric.
     *
     * @param dotKey key in dot notation
     * @return short value or {@code 0}
     */
    public short getShort(String dotKey) {
        Object value = getValue(dotKey);
        return (value instanceof Number n) ? n.shortValue() : 0;
    }

    /**
     * Sets a short value.
     *
     * @param dotKey     key in dot notation
     * @param shortValue value to set
     */
    public void setShort(String dotKey, short shortValue) {
        setValue(dotKey, shortValue);
    }

    /**
     * Returns a byte value or {@code 0} if missing or not numeric.
     *
     * @param dotKey key in dot notation
     * @return byte value or {@code 0}
     */
    public byte getByte(String dotKey) {
        Object value = getValue(dotKey);
        return (value instanceof Number n) ? n.byteValue() : 0;
    }

    /**
     * Sets a byte value.
     *
     * @param dotKey    key in dot notation
     * @param byteValue value to set
     */
    public void setByte(String dotKey, byte byteValue) {
        setValue(dotKey, byteValue);
    }

    /**
     * Returns a boolean value or {@code false} if missing or not a boolean.
     *
     * @param dotKey key in dot notation
     * @return boolean value or {@code false}
     */
    public boolean getBoolean(String dotKey) {
        Object value = getValue(dotKey);
        return (value instanceof Boolean b) ? b : false;
    }

    /**
     * Sets a boolean value.
     *
     * @param dotKey       key in dot notation
     * @param booleanValue value to set
     */
    public void setBoolean(String dotKey, boolean booleanValue) {
        setValue(dotKey, booleanValue);
    }

    /**
     * Returns a double value or {@code 0.0} if missing or not numeric.
     *
     * @param dotKey key in dot notation
     * @return double value or {@code 0.0}
     */
    public double getDouble(String dotKey) {
        Object value = getValue(dotKey);
        return (value instanceof Number n) ? n.doubleValue() : 0.0;
    }

    /**
     * Sets a double value.
     *
     * @param dotKey      key in dot notation
     * @param doubleValue value to set
     */
    public void setDouble(String dotKey, double doubleValue) {
        setValue(dotKey, doubleValue);
    }

    /**
     * Returns a string value or {@code null} if missing or not a string.
     *
     * @param dotKey key in dot notation
     * @return string value or {@code null}
     */
    public String getString(String dotKey) {
        Object value = getValue(dotKey);
        return (value instanceof String s) ? s : null;
    }

    /**
     * Sets a string value.
     *
     * @param dotKey      key in dot notation
     * @param stringValue value to set; may be {@code null}
     */
    public void setString(String dotKey, String stringValue) {
        setValue(dotKey, stringValue);
    }

    /**
     * Returns a single char from a string value or {@code '\u0000'} if missing/empty/not a string.
     *
     * @param dotKey key in dot notation
     * @return first char of the string or {@code '\u0000'}
     */
    public char getChar(String dotKey) {
        Object value = getValue(dotKey);
        return (value instanceof String s && !s.isEmpty()) ? s.charAt(0) : '\u0000';
    }

    /**
     * Sets a char value as a single-character string.
     *
     * @param dotKey    key in dot notation
     * @param charValue char to store
     */
    public void setChar(String dotKey, char charValue) {
        setValue(dotKey, String.valueOf(charValue));
    }

    /**
     * Returns map keys stored under the given dot-path.
     *
     * @param dotKey key in dot notation
     * @return immutable set of keys, or empty set if the value is missing or not a map
     */
    public Set<String> getMapKeys(String dotKey) {
        Map<String, Object> map = getMap(dotKey);
        return map.isEmpty() ? Set.of() : Set.copyOf(map.keySet());
    }

    /**
     * Returns map values stored under the given dot-path.
     *
     * @param dotKey key in dot notation
     * @return immutable collection of values, or empty collection if missing or not a map
     */
    public Collection<Object> getMapValues(String dotKey) {
        Map<String, Object> map = getMap(dotKey);
        return map.isEmpty() ? List.of() : List.copyOf(map.values());
    }

    /**
     * Stores a map value under the given dot-path.
     *
     * @param dotKey   key in dot notation
     * @param mapValue map value to store; may be {@code null}
     */
    public void setMap(String dotKey, Map<String, Object> mapValue) {
        setValue(dotKey, mapValue);
    }

    /**
     * Returns a defensive copy of a map stored under the given dot-path.
     *
     * @param dotKey key in dot notation
     * @return map copy or empty map if missing/not a map
     */
    public Map<String, Object> getMap(String dotKey) {
        Object value = getValue(dotKey);
        if (!(value instanceof Map<?, ?> raw)) {
            return Map.of();
        }
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> e : raw.entrySet()) {
            Object k = e.getKey();
            if (k instanceof String key) {
                result.put(key, e.getValue());
            }
        }
        return result;
    }

    /**
     * Stores a map of string values under the given dot-path.
     *
     * @param dotKey    key in dot notation
     * @param stringMap map to store
     */
    public void setStringMap(String dotKey, Map<String, String> stringMap) {
        setValue(dotKey, stringMap);
    }

    /**
     * Returns a defensive copy of a string map stored under the given dot-path.
     *
     * @param dotKey key in dot notation
     * @return map copy or empty map if missing/not a map
     */
    public Map<String, String> getStringMap(String dotKey) {
        return castMap(dotKey, String.class);
    }

    /**
     * Stores a map of double values under the given dot-path.
     *
     * @param dotKey    key in dot notation
     * @param doubleMap map to store
     */
    public void setDoubleMap(String dotKey, Map<String, Double> doubleMap) {
        setValue(dotKey, doubleMap);
    }

    /**
     * Returns a defensive copy of a double map stored under the given dot-path.
     *
     * @param dotKey key in dot notation
     * @return map copy or empty map if missing/not a map
     */
    public Map<String, Double> getDoubleMap(String dotKey) {
        return castMap(dotKey, Double.class);
    }

    /**
     * Stores a map of integer values under the given dot-path.
     *
     * @param dotKey key in dot notation
     * @param intMap map to store
     */
    public void setIntMap(String dotKey, Map<String, Integer> intMap) {
        setValue(dotKey, intMap);
    }

    /**
     * Returns a defensive copy of an integer map stored under the given dot-path.
     *
     * @param dotKey key in dot notation
     * @return map copy or empty map if missing/not a map
     */
    public Map<String, Integer> getIntMap(String dotKey) {
        return castMap(dotKey, Integer.class);
    }

    /**
     * Stores a map of boolean values under the given dot-path.
     *
     * @param dotKey     key in dot notation
     * @param booleanMap map to store
     */
    public void setBooleanMap(String dotKey, Map<String, Boolean> booleanMap) {
        setValue(dotKey, booleanMap);
    }

    /**
     * Returns a defensive copy of a boolean map stored under the given dot-path.
     *
     * @param dotKey key in dot notation
     * @return map copy or empty map if missing/not a map
     */
    public Map<String, Boolean> getBooleanMap(String dotKey) {
        return castMap(dotKey, Boolean.class);
    }

    /**
     * Returns a list stored under the given dot-path, or an empty list if missing/not a list.
     *
     * @param dotKey key in dot notation
     * @return list copy, never {@code null}
     */
    @SuppressWarnings("unchecked")
    public List<Object> getList(String dotKey) {
        Object value = getValue(dotKey);
        if (!(value instanceof List<?> raw)) {
            return List.of();
        }
        return List.copyOf((List<Object>) raw);
    }

    /**
     * Stores a list under the given dot-path.
     *
     * @param dotKey    key in dot notation
     * @param listValue list to store
     */
    public void setList(String dotKey, List<Object> listValue) {
        setValue(dotKey, listValue);
    }

    /**
     * Returns a list of strings stored under the given dot-path.
     *
     * @param dotKey key in dot notation
     * @return list of strings, never {@code null}
     */
    public List<String> getStringList(String dotKey) {
        return getList(dotKey).stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .toList();
    }

    /**
     * Stores a list of strings under the given dot-path.
     *
     * @param dotKey key in dot notation
     * @param values list of strings to store
     */
    public void setStringList(String dotKey, List<String> values) {
        setValue(dotKey, values);
    }

    /**
     * Returns a list of doubles stored under the given dot-path.
     *
     * @param dotKey key in dot notation
     * @return list of doubles, never {@code null}
     */
    public List<Double> getDoubleList(String dotKey) {
        return getList(dotKey).stream()
                .filter(Double.class::isInstance)
                .map(Double.class::cast)
                .toList();
    }

    /**
     * Stores a list of doubles under the given dot-path.
     *
     * @param dotKey key in dot notation
     * @param values list of doubles to store
     */
    public void setDoubleList(String dotKey, List<Double> values) {
        setValue(dotKey, values);
    }

    /**
     * Returns a list of longs stored under the given dot-path.
     *
     * @param dotKey key in dot notation
     * @return list of longs, never {@code null}
     */
    public List<Long> getLongList(String dotKey) {
        return getList(dotKey).stream()
                .filter(Number.class::isInstance)
                .map(Number.class::cast)
                .map(Number::longValue)
                .toList();
    }

    /**
     * Stores a list of longs under the given dot-path.
     *
     * @param dotKey key in dot notation
     * @param values list of longs to store
     */
    public void setLongList(String dotKey, List<Long> values) {
        setValue(dotKey, values);
    }

    /**
     * Returns a list of integers stored under the given dot-path.
     *
     * @param dotKey key in dot notation
     * @return list of integers, never {@code null}
     */
    public List<Integer> getIntegerList(String dotKey) {
        return getList(dotKey).stream()
                .filter(Integer.class::isInstance)
                .map(Integer.class::cast)
                .toList();
    }

    /**
     * Stores a list of integers under the given dot-path.
     *
     * @param dotKey key in dot notation
     * @param values list of integers to store
     */
    public void setIntegerList(String dotKey, List<Integer> values) {
        setValue(dotKey, values);
    }

    /**
     * Returns a list of characters by taking the first character from each string element.
     *
     * @param dotKey key in dot notation
     * @return list of characters, never {@code null}
     */
    public List<Character> getCharacterList(String dotKey) {
        return getList(dotKey).stream()
                .filter(v -> v instanceof String s && !s.isEmpty())
                .map(v -> ((String) v).charAt(0))
                .toList();
    }

    /**
     * Stores a list of characters as single-character strings.
     *
     * @param dotKey key in dot notation
     * @param values list of characters
     */
    public void setCharacterList(String dotKey, List<Character> values) {
        Objects.requireNonNull(values, "Values must not be null");
        List<String> strings = values.stream().map(String::valueOf).toList();
        setValue(dotKey, strings);
    }

    /**
     * Returns a list of booleans stored under the given dot-path.
     *
     * @param dotKey key in dot notation
     * @return list of booleans, never {@code null}
     */
    public List<Boolean> getBooleanList(String dotKey) {
        return getList(dotKey).stream()
                .filter(Boolean.class::isInstance)
                .map(Boolean.class::cast)
                .toList();
    }

    /**
     * Stores a list of booleans under the given dot-path.
     *
     * @param dotKey key in dot notation
     * @param values list of booleans
     */
    public void setBooleanList(String dotKey, List<Boolean> values) {
        setValue(dotKey, values);
    }

    /**
     * Returns a list of shorts stored under the given dot-path.
     *
     * @param dotKey key in dot notation
     * @return list of shorts, never {@code null}
     */
    public List<Short> getShortList(String dotKey) {
        return getList(dotKey).stream()
                .filter(Number.class::isInstance)
                .map(Number.class::cast)
                .map(Number::shortValue)
                .toList();
    }

    /**
     * Stores a list of shorts under the given dot-path.
     *
     * @param dotKey key in dot notation
     * @param values list of shorts
     */
    public void setShortList(String dotKey, List<Short> values) {
        setValue(dotKey, values);
    }

    /**
     * Returns a list of bytes stored under the given dot-path.
     *
     * @param dotKey key in dot notation
     * @return list of bytes, never {@code null}
     */
    public List<Byte> getByteList(String dotKey) {
        return getList(dotKey).stream()
                .filter(Number.class::isInstance)
                .map(Number.class::cast)
                .map(Number::byteValue)
                .toList();
    }

    /**
     * Stores a list of bytes under the given dot-path.
     *
     * @param dotKey key in dot notation
     * @param values list of bytes
     */
    public void setByteList(String dotKey, List<Byte> values) {
        setValue(dotKey, values);
    }

    /**
     * Loads YAML from a JAR entry into {@link #yamlData}.
     *
     * @param jarPath           filesystem path to the jar
     * @param internalEntryPath jar entry path
     */
    private void loadFromJar(Path jarPath, String internalEntryPath) {
        Path normalizedJar = jarPath.toAbsolutePath().normalize();
        if (!Files.isRegularFile(normalizedJar)) {
            throw new IllegalStateException("JAR file does not exist: " + normalizedJar);
        }

        try (JarFile jar = new JarFile(normalizedJar.toFile())) {
            JarEntry entry = jar.getJarEntry(internalEntryPath);
            if (entry == null) {
                Logger.debug("YAML entry '{}' not found in JAR '{}'. Treating as empty YAML.", internalEntryPath, normalizedJar);
                return;
            }
            try (InputStream in = jar.getInputStream(entry)) {
                loadFromStream(in, "jar:" + normalizedJar + "!/" + internalEntryPath);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read YAML from JAR: " + normalizedJar + " entry=" + internalEntryPath, e);
        }
    }

    /**
     * Loads YAML data from the given stream and replaces {@link #yamlData}.
     *
     * @param yamlInputStream input stream with YAML content
     * @param source          display name for diagnostics/logs
     */
    private void loadFromStream(InputStream yamlInputStream, String source) {
        try (InputStream in = yamlInputStream) {
            Object loaded = DEFAULT_YAML.load(in);
            Map<String, Object> result;
            if (loaded == null) {
                result = new LinkedHashMap<>();
                Logger.debug("YAML source '{}' is empty.", source);
            } else if (loaded instanceof Map<?, ?> map) {
                result = new LinkedHashMap<>();
                for (Map.Entry<?, ?> e : map.entrySet()) {
                    if (e.getKey() instanceof String key) {
                        result.put(key, e.getValue());
                    }
                }
            } else {
                throw new IllegalStateException("YAML root must be a mapping, but was: " + loaded.getClass().getName());
            }

            synchronized (lock) {
                this.yamlData = result;
            }
            Logger.debug("Loaded YAML '{}' (keys={})", source, result.size());
        } catch (RuntimeException | IOException e) {
            Logger.error("Failed to parse YAML from '{}'.", source, e);
            throw new IllegalStateException("Failed to parse YAML: " + source, e);
        }
    }

    /**
     * Saves YAML to the given path using stable dumper options and UTF-8 encoding.
     *
     * @param outputPath output path
     */
    private void save(Path outputPath) {
        Objects.requireNonNull(outputPath, "Output path must not be null");

        Path normalized = outputPath.toAbsolutePath().normalize();
        Path parent = normalized.getParent();
        try {
            if (parent != null) {
                Files.createDirectories(parent);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to create directories for: " + normalized, e);
        }

        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setDefaultScalarStyle(DumperOptions.ScalarStyle.PLAIN);
        options.setProcessComments(true);

        Yaml yaml = new Yaml(options);

        Map<String, Object> snapshot;
        synchronized (lock) {
            snapshot = deepCopyMap(yamlData);
        }

        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(Files.newOutputStream(normalized), StandardCharsets.UTF_8))) {
            yaml.dump(snapshot, writer);
            Logger.debug("Saved YAML to '{}'", normalized);
        } catch (IOException e) {
            Logger.error("Failed to save YAML to '{}'.", normalized, e);
            throw new IllegalStateException("Failed to save YAML to: " + normalized, e);
        }
    }

    /**
     * Validates a dot-path and returns its segments.
     *
     * <p>
     * Rules:
     * </p>
     * <ul>
     *   <li>{@code null} is rejected</li>
     *   <li>blank strings are rejected</li>
     *   <li>empty segments are rejected (e.g. {@code "a..b"}, {@code ".a"}, {@code "a."})</li>
     *   <li>each segment is trimmed</li>
     * </ul>
     *
     * @param dotKey key in dot notation
     * @return non-empty segments array
     * @throws IllegalArgumentException if invalid
     */
    private static String[] validateAndSplitDotPath(String dotKey) {
        if (dotKey == null) {
            throw new IllegalArgumentException("Key must not be null.");
        }

        String trimmed = dotKey.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Key must not be blank.");
        }

        // Fast invalid checks: leading/trailing dot or double dots.
        if (trimmed.charAt(0) == '.' || trimmed.charAt(trimmed.length() - 1) == '.' || trimmed.contains("..")) {
            throw new IllegalArgumentException("Invalid key (empty segment): " + dotKey);
        }

        String[] raw = trimmed.split("\\.");
        for (int i = 0; i < raw.length; i++) {
            String seg = raw[i].trim();
            if (seg.isEmpty()) {
                throw new IllegalArgumentException("Invalid key (empty segment): " + dotKey);
            }
            raw[i] = seg;
        }
        return raw;
    }

    /**
     * Recursively merges {@code source} into {@code target}.
     *
     * @param target map to mutate
     * @param source map to read from
     */
    @SuppressWarnings("unchecked")
    private static void mergeRecursive(Map<String, Object> target, Map<String, Object> source) {
        for (Map.Entry<String, Object> e : source.entrySet()) {
            String key = e.getKey();
            Object sourceValue = e.getValue();

            Object targetValue = target.get(key);
            if (sourceValue instanceof Map<?, ?> srcMap && targetValue instanceof Map<?, ?> tgtMap) {
                mergeRecursive((Map<String, Object>) tgtMap, (Map<String, Object>) srcMap);
            } else {
                target.put(key, sourceValue);
            }
        }
    }

    /**
     * Parses a dot-path key into segments and validates it.
     *
     * @param dotKey key in dot notation
     * @return list of segments
     * @throws IllegalArgumentException if the key is blank or has empty segments
     */
    private static List<String> parseDotPath(String dotKey) {
        String trimmed = dotKey.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Key must not be blank.");
        }

        String[] raw = trimmed.split("\\.");
        ArrayList<String> segments = new ArrayList<>(raw.length);
        for (String part : raw) {
            String seg = part.trim();
            if (seg.isEmpty()) {
                throw new IllegalArgumentException("Invalid key (empty segment): " + dotKey);
            }
            segments.add(seg);
        }
        return segments;
    }

    /**
     * Extracts a file name from a path-like string.
     *
     * @param path path string
     * @return file name
     */
    private static String extractFileName(String path) {
        int idx = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
        return (idx >= 0 && idx < path.length() - 1) ? path.substring(idx + 1) : path;
    }

    /**
     * Creates a deep copy of a YAML map to prevent external mutations.
     *
     * @param source source map
     * @return deep copy
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> deepCopyMap(Map<String, Object> source) {
        LinkedHashMap<String, Object> copy = new LinkedHashMap<>(Math.max(16, source.size()));
        for (Map.Entry<String, Object> e : source.entrySet()) {
            Object v = e.getValue();
            if (v instanceof Map<?, ?> m) {
                copy.put(e.getKey(), deepCopyMap((Map<String, Object>) m));
            } else if (v instanceof List<?> list) {
                copy.put(e.getKey(), deepCopyList(list));
            } else {
                copy.put(e.getKey(), v);
            }
        }
        return copy;
    }

    /**
     * Creates a deep copy of a YAML list.
     *
     * @param source source list
     * @return deep copy list
     */
    @SuppressWarnings("unchecked")
    private static List<Object> deepCopyList(List<?> source) {
        ArrayList<Object> copy = new ArrayList<>(source.size());
        for (Object v : source) {
            if (v instanceof Map<?, ?> m) {
                copy.add(deepCopyMap((Map<String, Object>) m));
            } else if (v instanceof List<?> list) {
                copy.add(deepCopyList(list));
            } else {
                copy.add(v);
            }
        }
        return copy;
    }

    /**
     * Reads a map under {@code dotKey} and filters values by the provided type.
     *
     * @param dotKey    key in dot notation
     * @param valueType expected value type
     * @param <T>       value type
     * @return immutable map of matching entries or empty map
     */
    private <T> Map<String, T> castMap(String dotKey, Class<T> valueType) {
        Objects.requireNonNull(valueType, "Value type must not be null");

        Object raw = getValue(dotKey);
        if (!(raw instanceof Map<?, ?> map)) {
            return Map.of();
        }

        LinkedHashMap<String, T> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> e : map.entrySet()) {
            if (!(e.getKey() instanceof String key)) {
                continue;
            }
            Object v = e.getValue();
            if (valueType.isInstance(v)) {
                result.put(key, valueType.cast(v));
            }
        }
        return result.isEmpty() ? Map.of() : Map.copyOf(result);
    }
}