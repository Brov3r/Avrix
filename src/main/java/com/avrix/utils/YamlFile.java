package com.avrix.utils;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * This class represents a YAML file and provides methods to read, write, and manipulate its contents.
 */
public class YamlFile {
    private Map<String, Object> yamlData = new LinkedHashMap<>(); // Deserialized YAML file content as a dictionary
    private Path filePath; // Path to the YAML file
    private String fileName; // YAML file name

    /**
     * Constructor to load a YAML file from a JAR file.
     *
     * @param jarFilePath      the path to the JAR file
     * @param internalFilePath the internal path to the YAML file within the JAR
     * @throws IOException if an I/O error occurs
     */
    public YamlFile(String jarFilePath, String internalFilePath) throws IOException {
        URL jarUrl = new URL("jar:file:" + jarFilePath + "!/" + internalFilePath);
        try (InputStream inputStream = jarUrl.openStream()) {
            Yaml yaml = new Yaml();
            Map<String, Object> loadData = yaml.load(inputStream);
            if (loadData != null) {
                this.yamlData = loadData;
            }
            this.filePath = Paths.get(jarFilePath).resolve(internalFilePath);
            this.fileName = internalFilePath;
        } catch (IOException e) {
            System.out.printf("[!] File '%s' not found inside JAR file '%s'!%n", internalFilePath, jarFilePath);
        }
    }

    /**
     * Constructor to load a YAML file from an {@link InputStream}.
     *
     * @param inputStream the {@link InputStream} to load the YAML file from
     * @param filePath    the path to the YAML file
     * @param fileName    the name of the YAML file
     */
    public YamlFile(InputStream inputStream, Path filePath, String fileName) {
        try (inputStream) {
            Yaml yaml = new Yaml();
            Map<String, Object> loadData = yaml.load(inputStream);
            if (loadData != null) {
                this.yamlData = loadData;
            }
            this.filePath = filePath;
            this.fileName = fileName;
        } catch (Exception e) {
            System.out.println("[!] Error occurred while loading YAML data from InputStream: " + e.getMessage());
        }
    }

    /**
     * Constructor to load a YAML file from a {@link Path}.
     *
     * @param path the {@link Path} to the YAML file
     * @throws IOException if an I/O error occurs
     */
    public YamlFile(Path path) throws IOException {
        this(Files.newInputStream(path), path, path.getFileName().toString());
    }

    /**
     * Constructor to load a YAML file from a {@link URL}.
     *
     * @param filePath the {@link URL} to the YAML file
     * @throws IOException        if an I/O error occurs
     * @throws URISyntaxException if the {@link URL} is malformed
     */
    public YamlFile(URL filePath) throws IOException, URISyntaxException {
        this(filePath.openStream(), Paths.get(filePath.toURI()), Paths.get(filePath.toURI()).getFileName().toString());
    }

    /**
     * Constructor to load a YAML file from a {@link URI}.
     *
     * @param fileUri the {@link URI} to the YAML file
     * @throws IOException if an I/O error occurs
     */
    public YamlFile(URI fileUri) throws IOException {
        this(fileUri.toURL().openStream(), Paths.get(fileUri), Paths.get(fileUri).getFileName().toString());
    }

    /**
     * Constructor to load a YAML file from a {@link File}.
     *
     * @param file the YAML file
     * @throws IOException if an I/O error occurs
     */
    public YamlFile(File file) throws IOException {
        this(file.toURI());
    }

    /**
     * Constructor to load a YAML file from a file path.
     *
     * @param filePath the path to the YAML file
     * @throws IOException if an I/O error occurs
     */
    public YamlFile(String filePath) throws IOException {
        this(new File(filePath));
    }

    /**
     * Returns the file path of the YAML file.
     *
     * @return the file path
     */
    public Path getFilePath() {
        return this.filePath;
    }

    /**
     * Returns the file name of the YAML file.
     *
     * @return the file name
     */
    public String getFileName() {
        return this.fileName;
    }

    /**
     * Saves the YAML file to a specified file path.
     *
     * @param path the path to save the YAML file to
     */
    public synchronized void save(String path) {
        File filePath = new File(path);

        if (filePath.getParentFile() != null && !filePath.getParentFile().exists()) {
            if (!filePath.getParentFile().mkdirs()) {
                System.err.println("[!] Failed to create directories for the path: " + path);
                return;
            }
        }

        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setDefaultScalarStyle(DumperOptions.ScalarStyle.PLAIN);
        options.setProcessComments(true);

        Yaml yaml = new Yaml(options);
        try (FileWriter writer = new FileWriter(filePath)) {
            yaml.dump(yamlData, writer);
        } catch (IOException e) {
            System.err.println("[!] Error occurred while saving YAML file: " + e.getMessage());
        }
    }

    /**
     * Saves the YAML file to its original file path.
     */
    public synchronized void save() {
        this.save(this.filePath.toString());
    }

    /**
     * Loads a YAML file from a File.
     *
     * @param filePath the YAML file
     * @return the loaded YamlFile object, or null if the file does not exist
     */
    public static YamlFile load(File filePath) {
        try {
            if (filePath.exists()) {
                return new YamlFile(filePath);
            } else {
                System.out.printf("[!] The YAML file '%s' does not exist.%n", filePath.getName());
                return null;
            }
        } catch (IOException e) {
            System.out.println("[!] Error loading YAML file: " + e.getMessage());
            return null;
        }
    }

    /**
     * Loads a YAML file from a {@link Path}.
     *
     * @param filePath the path to the YAML file
     * @return the loaded YamlFile object
     */
    public static YamlFile load(Path filePath) {
        return load(filePath.toFile());
    }

    /**
     * Loads a YAML file from a file path.
     *
     * @param filePath the path to the YAML file
     * @return the loaded YamlFile object
     */
    public static YamlFile load(String filePath) {
        return load(new File(filePath));
    }

    /**
     * Creates a new YAML file.
     *
     * @param filePath the YAML file to create
     * @return the created YamlFile object, or null if the file already exists or an error occurs
     */
    public static YamlFile create(File filePath) {
        try {
            if (filePath.createNewFile()) {
                return new YamlFile(filePath.getAbsolutePath());
            } else {
                System.out.printf("[!] The YAML file '%s' already exists.%n", filePath.getName());
                return null;
            }
        } catch (IOException e) {
            System.out.println("[!] Error creating YAML file: " + e.getMessage());
            return null;
        }
    }

    /**
     * Creates a new YAML file from a {@link Path}.
     *
     * @param filePath the path to the YAML file to create
     * @return the created YamlFile object
     */
    public static YamlFile create(Path filePath) {
        return create(filePath.toFile());
    }

    /**
     * Creates a new YAML file from a file path.
     *
     * @param filePath the path to the YAML file to create
     * @return the created YamlFile object
     */
    public static YamlFile create(String filePath) {
        return create(new File(filePath));
    }

    /**
     * Removes a key from the YAML file.
     *
     * @param key the key to remove
     */
    @SuppressWarnings("unchecked")
    public final synchronized void remove(String key) {
        String[] keys = key.split("\\.");
        Map<String, Object> data = yamlData;

        for (int i = 0; i < keys.length - 1; i++) {
            data = (Map<String, Object>) data.get(keys[i]);
            if (data == null) {
                return;
            }
        }

        data.remove(keys[keys.length - 1]);
    }

    /**
     * Sets a value in the YAML file.
     *
     * @param key   the key to set
     * @param value the value to set
     */
    @SuppressWarnings("unchecked")
    public synchronized void setValue(String key, Object value) {
        String[] keys = key.split("\\.");
        Map<String, Object> data = yamlData;

        for (int i = 0; i < keys.length - 1; i++) {
            Object current = data.get(keys[i]);

            if (!(current instanceof Map)) {
                current = new LinkedHashMap<String, Object>();
                data.put(keys[i], current);
            }

            data = (Map<String, Object>) current;
        }

        data.put(keys[keys.length - 1], value);
    }

    /**
     * Gets a value from the YAML file.
     *
     * @param key the key to get
     * @return the value associated with the key, or null if the key does not exist
     */
    @SuppressWarnings("unchecked")
    public synchronized Object getValue(String key) {
        String[] keys = key.split("\\.");
        Map<String, Object> data = yamlData;

        for (int i = 0; i < keys.length - 1; i++) {
            Object obj = data.get(keys[i]);

            if (!(obj instanceof Map)) return null;

            data = (Map<String, Object>) obj;
        }
        return data.get(keys[keys.length - 1]);
    }

    /**
     * Returns all the data in the YAML file.
     *
     * @return a map containing all the data in the YAML file
     */
    public final Map<String, Object> getAll() {
        return new LinkedHashMap<>(yamlData);
    }

    /**
     * Checks if a key exists in the YAML file.
     *
     * @param key the key to check
     * @return true if the key exists, false otherwise
     */
    public final synchronized boolean contains(String key) {
        return getValue(key) != null;
    }

    /**
     * Clears all the data in the YAML file.
     */
    public final synchronized void clear() {
        yamlData.clear();
    }

    /**
     * Checks if the YAML file is empty.
     *
     * @return true if the YAML file is empty, false otherwise
     */
    public final synchronized boolean isEmpty() {
        return yamlData.isEmpty();
    }

    /**
     * Merges a map of data into the YAML file.
     *
     * @param data the data to merge
     */
    public final synchronized void merge(Map<String, Object> data) {
        mergeRecursive(yamlData, data);
    }

    /**
     * Merges another {@link YamlFile} into this YAML file.
     *
     * @param yamlFile the {@link YamlFile} to merge
     */
    public final synchronized void merge(YamlFile yamlFile) {
        Map<String, Object> data = yamlFile.getAll();
        merge(data);
    }

    /**
     * Recursively combines two configurations.
     *
     * @param targetData The target data to which the keys and values are added.
     * @param sourceData Another data from which the keys and values are copied.
     */
    @SuppressWarnings("unchecked")
    private synchronized void mergeRecursive(Map<String, Object> targetData, Map<String, Object> sourceData) {
        for (Map.Entry<String, Object> entry : sourceData.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (value instanceof Map && targetData.containsKey(key) && targetData.get(key) instanceof Map) {
                // If the value is also a map and the key is already present in the target map,
                // then call this method recursively to combine nested maps
                mergeRecursive((Map<String, Object>) targetData.get(key), (Map<String, Object>) value);
            } else {
                // Otherwise, just replace the value in the target map
                targetData.put(key, value);
            }
        }
    }

    /**
     * Gets an integer value from the YAML file.
     *
     * @param key the key to get the integer value from
     * @return the integer value associated with the key, or 0 if the key does not exist or is not a number
     */
    public final int getInt(String key) {
        Object value = getValue(key);
        return value instanceof Number ? ((Number) value).intValue() : 0;
    }

    /**
     * Sets an integer value in the YAML file.
     *
     * @param key   the key to set the integer value for
     * @param value the integer value to set
     */
    public final void setInt(String key, int value) {
        setValue(key, value);
    }

    /**
     * Gets a long value from the YAML file.
     *
     * @param key the key to get the long value from
     * @return the long value associated with the key, or 0L if the key does not exist or is not a number
     */
    public final long getLong(String key) {
        Object value = getValue(key);
        return value instanceof Number ? ((Number) value).longValue() : 0L;
    }

    /**
     * Sets a long value in the YAML file.
     *
     * @param key   the key to set the long value for
     * @param value the long value to set
     */
    public final void setLong(String key, long value) {
        setValue(key, value);
    }

    /**
     * Gets a short value from the YAML file.
     *
     * @param key the key to get the short value from
     * @return the short value associated with the key, or 0 if the key does not exist or is not a number
     */
    public final short getShort(String key) {
        Object value = getValue(key);
        return value instanceof Number ? ((Number) value).shortValue() : 0;
    }

    /**
     * Sets a short value in the YAML file.
     *
     * @param key   the key to set the short value for
     * @param value the short value to set
     */
    public final void setShort(String key, short value) {
        setValue(key, value);
    }

    /**
     * Gets a byte value from the YAML file.
     *
     * @param key the key to get the byte value from
     * @return the byte value associated with the key, or 0 if the key does not exist or is not a number
     */
    public final byte getByte(String key) {
        Object value = getValue(key);
        return value instanceof Number ? ((Number) value).byteValue() : 0;
    }

    /**
     * Sets a byte value in the YAML file.
     *
     * @param key   the key to set the byte value for
     * @param value the byte value to set
     */
    public final void setByte(String key, byte value) {
        setValue(key, value);
    }

    /**
     * Gets a boolean value from the YAML file.
     *
     * @param key the key to get the boolean value from
     * @return the boolean value associated with the key, or false if the key does not exist or is not a boolean
     */
    public final boolean getBoolean(String key) {
        Object value = getValue(key);
        return value instanceof Boolean ? (Boolean) value : false;
    }

    /**
     * Sets a boolean value in the YAML file.
     *
     * @param key   the key to set the boolean value for
     * @param value the boolean value to set
     */
    public final void setBoolean(String key, boolean value) {
        setValue(key, value);
    }

    /**
     * Gets a double value from the YAML file.
     *
     * @param key the key to get the double value from
     * @return the double value associated with the key, or 0.0 if the key does not exist or is not a number
     */
    public final double getDouble(String key) {
        Object value = getValue(key);
        return value instanceof Number ? ((Number) value).doubleValue() : 0.0;
    }

    /**
     * Sets a double value in the YAML file.
     *
     * @param key   the key to set the double value for
     * @param value the double value to set
     */
    public final void setDouble(String key, double value) {
        setValue(key, value);
    }

    /**
     * Gets a string value from the YAML file.
     *
     * @param key the key to get the string value from
     * @return the string value associated with the key, or null if the key does not exist or is not a string
     */
    public final String getString(String key) {
        Object value = getValue(key);
        return value instanceof String ? (String) value : null;
    }

    /**
     * Sets a string value in the YAML file.
     *
     * @param key   the key to set the string value for
     * @param value the string value to set
     */
    public final void setString(String key, String value) {
        setValue(key, value);
    }

    /**
     * Gets a char value from the YAML file.
     *
     * @param key the key to get the char value from
     * @return the char value associated with the key, or '\u0000' if the key does not exist or is not a string
     */
    public final char getChar(String key) {
        Object value = getValue(key);
        return (value instanceof String && !((String) value).isEmpty()) ? ((String) value).charAt(0) : '\u0000';
    }

    /**
     * Sets a char value in the YAML file.
     *
     * @param key   the key to set the char value for
     * @param value the char value to set
     */
    public final void setChar(String key, char value) {
        setValue(key, String.valueOf(value));
    }


    /**
     * Retrieves the keys of the map stored under the specified key in the YAML file.
     *
     * @param key the key of the map in the YAML file
     * @return a set containing the keys of the map, or null if the key does not exist or does not contain a map
     */
    @SuppressWarnings("unchecked")
    public final Set<String> getMapKeys(String key) {
        Object object = getValue(key);

        if (object instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) object;
            return map.keySet();
        }

        return null;
    }

    /**
     * Retrieves the values of the map stored under the specified key in the YAML file.
     *
     * @param key the key of the map in the YAML file
     * @return a collection containing the values of the map, or null if the key does not exist or does not contain a map
     */
    @SuppressWarnings("unchecked")
    public final Collection<Object> getMapValues(String key) {
        Object object = getValue(key);
        if (object instanceof Map) {
            return ((Map<String, Object>) object).values();
        }
        return null;
    }

    /**
     * Sets a map in the YAML file under the specified key.
     *
     * @param key the key where the map should be set
     * @param map the map to set
     */
    public void setMap(String key, Map<String, Object> map) {
        setValue(key, map);
    }

    /**
     * Gets a map from the YAML file stored under the specified key.
     *
     * @param key the key of the map to retrieve
     * @return the map stored under the specified key, or null if the key does not exist or does not contain a map
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getMap(String key) {
        Object value = getValue(key);
        return (value instanceof Map) ? (Map<String, Object>) value : null;
    }

    /**
     * Sets a map of string values in the YAML file under the specified key.
     *
     * @param key the key where the map should be set
     * @param map the map of string values to set
     */
    public void setStringMap(String key, Map<String, String> map) {
        setValue(key, map);
    }

    /**
     * Gets a map of string values from the YAML file stored under the specified key.
     *
     * @param key the key of the map to retrieve
     * @return the map of string values stored under the specified key, or null if the key does not exist or does not contain a map
     */
    @SuppressWarnings("unchecked")
    public Map<String, String> getStringMap(String key) {
        Object value = getValue(key);
        return (value instanceof Map) ? (Map<String, String>) value : null;
    }

    /**
     * Sets a map of double values in the YAML file under the specified key.
     *
     * @param key the key where the map should be set
     * @param map the map of double values to set
     */
    public void setDoubleMap(String key, Map<String, Double> map) {
        setValue(key, map);
    }

    /**
     * Gets a map of double values from the YAML file stored under the specified key.
     *
     * @param key the key of the map to retrieve
     * @return the map of double values stored under the specified key, or null if the key does not exist or does not contain a map
     */
    @SuppressWarnings("unchecked")
    public Map<String, Double> getDoubleMap(String key) {
        Object value = getValue(key);
        return (value instanceof Map) ? (Map<String, Double>) value : null;
    }

    /**
     * Sets a map of integer values in the YAML file under the specified key.
     *
     * @param key the key where the map should be set
     * @param map the map of integer values to set
     */
    public void setIntMap(String key, Map<String, Integer> map) {
        setValue(key, map);
    }

    /**
     * Gets a map of integer values from the YAML file stored under the specified key.
     *
     * @param key the key of the map to retrieve
     * @return the map of integer values stored under the specified key, or null if the key does not exist or does not contain a map
     */
    @SuppressWarnings("unchecked")
    public Map<String, Integer> getIntMap(String key) {
        Object value = getValue(key);
        return (value instanceof Map) ? (Map<String, Integer>) value : null;
    }

    /**
     * Sets a map of boolean values in the YAML file under the specified key.
     *
     * @param key the key where the map should be set
     * @param map the map of boolean values to set
     */
    public void setBooleanMap(String key, Map<String, Boolean> map) {
        setValue(key, map);
    }

    /**
     * Gets a map of boolean values from the YAML file stored under the specified key.
     *
     * @param key the key of the map to retrieve
     * @return the map of boolean values stored under the specified key, or null if the key does not exist or does not contain a map
     */
    @SuppressWarnings("unchecked")
    public Map<String, Boolean> getBooleanMap(String key) {
        Object value = getValue(key);
        return (value instanceof Map) ? (Map<String, Boolean>) value : null;
    }

    /**
     * Gets a list of values from the YAML file.
     *
     * @param key the key to get the list of values from
     * @return the list of values associated with the key, or an empty list if the key does not exist or is not a list
     */
    @SuppressWarnings("unchecked")
    public final List<Object> getList(String key) {
        Object value = getValue(key);
        return value instanceof List ? (List<Object>) value : new ArrayList<>();
    }

    /**
     * Sets a list of values in the YAML file.
     *
     * @param key   the key to set the list of values for
     * @param value the list of values to set
     */
    public final void setList(String key, List<Object> value) {
        setValue(key, value);
    }

    /**
     * Gets a list of strings from the YAML file.
     *
     * @param key the key to get the list of strings from
     * @return the list of strings associated with the key, or an empty list if the key does not exist or is not a list
     */
    public final List<String> getStringList(String key) {
        return getList(key).stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .collect(Collectors.toList());
    }

    /**
     * Sets a list of strings in the YAML file.
     *
     * @param key   the key to set the list of strings for
     * @param value the list of strings to set
     */
    public final void setStringList(String key, List<String> value) {
        setValue(key, value);
    }

    /**
     * Gets a list of doubles from the YAML file.
     *
     * @param key the key to get the list of doubles from
     * @return the list of doubles associated with the key, or an empty list if the key does not exist or is not a list
     */
    public final List<Double> getDoubleList(String key) {
        return getList(key).stream()
                .filter(Double.class::isInstance)
                .map(Double.class::cast)
                .collect(Collectors.toList());
    }

    /**
     * Sets a list of doubles in the YAML file.
     *
     * @param key   the key to set the list of doubles for
     * @param value the list of doubles to set
     */
    public final void setDoubleList(String key, List<Double> value) {
        setValue(key, value);
    }

    /**
     * Gets a list of longs from the YAML file.
     *
     * @param key the key to get the list of longs from
     * @return the list of longs associated with the key, or an empty list if the key does not exist or is not a list
     */
    public final List<Long> getLongList(String key) {
        return getList(key).stream()
                .filter(Number.class::isInstance)
                .map(Number.class::cast)
                .map(Number::longValue)
                .collect(Collectors.toList());
    }

    /**
     * Sets a list of longs in the YAML file.
     *
     * @param key   the key to set the list of longs for
     * @param value the list of longs to set
     */
    public final void setLongList(String key, List<Long> value) {
        setValue(key, value);
    }

    /**
     * Gets a list of integers from the YAML file.
     *
     * @param key the key to get the list of integers from
     * @return the list of integers associated with the key, or an empty list if the key does not exist or is not a list
     */
    public final List<Integer> getIntegerList(String key) {
        return getList(key).stream()
                .filter(Integer.class::isInstance)
                .map(Integer.class::cast)
                .collect(Collectors.toList());
    }

    /**
     * Sets a list of integers in the YAML file.
     *
     * @param key   the key to set the list of integers for
     * @param value the list of integers to set
     */
    public final void setIntegerList(String key, List<Integer> value) {
        setValue(key, value);
    }

    /**
     * Gets a list of characters from the YAML file.
     *
     * @param key the key to get the list of characters from
     * @return the list of characters associated with the key, or an empty list if the key does not exist or is not a list
     */
    public final List<Character> getCharacterList(String key) {
        return getList(key).stream()
                .filter(item -> item instanceof String && !((String) item).isEmpty())
                .map(item -> ((String) item).charAt(0))
                .collect(Collectors.toList());
    }

    /**
     * Sets a list of characters in the YAML file.
     *
     * @param key   the key to set the list of characters for
     * @param value the list of characters to set
     */
    public final void setCharacterList(String key, List<Character> value) {
        List<String> stringList = value.stream()
                .map(String::valueOf)
                .collect(Collectors.toList());
        setValue(key, stringList);
    }

    /**
     * Gets a list of booleans from the YAML file.
     *
     * @param key the key to get the list of booleans from
     * @return the list of booleans associated with the key, or an empty list if the key does not exist or is not a list
     */
    public final List<Boolean> getBooleanList(String key) {
        return getList(key).stream()
                .filter(Boolean.class::isInstance)
                .map(Boolean.class::cast)
                .collect(Collectors.toList());
    }

    /**
     * Sets a list of booleans in the YAML file.
     *
     * @param key   the key to set the list of booleans for
     * @param value the list of booleans to set
     */
    public final void setBooleanList(String key, List<Boolean> value) {
        setValue(key, value);
    }

    /**
     * Gets a list of shorts from the YAML file.
     *
     * @param key the key to get the list of shorts from
     * @return the list of shorts associated with the key, or an empty list if the key does not exist or is not a list
     */
    public final List<Short> getShortList(String key) {
        return getList(key).stream()
                .filter(Number.class::isInstance)
                .map(Number.class::cast)
                .map(Number::shortValue)
                .collect(Collectors.toList());
    }

    /**
     * Sets a list of shorts in the YAML file.
     *
     * @param key   the key to set the list of shorts for
     * @param value the list of shorts to set
     */
    public final void setShortList(String key, List<Short> value) {
        setValue(key, value);
    }

    /**
     * Gets a list of bytes from the YAML file.
     *
     * @param key the key to get the list of bytes from
     * @return the list of bytes associated with the key, or an empty list if the key does not exist or is not a list
     */
    public final List<Byte> getByteList(String key) {
        return getList(key).stream()
                .filter(Number.class::isInstance)
                .map(Number.class::cast)
                .map(Number::byteValue)
                .collect(Collectors.toList());
    }


    /**
     * Sets a list of bytes in the YAML file.
     *
     * @param key   the key to set the list of bytes for
     * @param value the list of bytes to set
     */
    public final void setByteList(String key, List<Byte> value) {
        setValue(key, value);
    }
}