package com.avrix.plugins;

import com.avrix.enums.Environment;
import com.avrix.enums.PluginType;
import com.avrix.utils.YamlFile;

import java.io.File;
import java.nio.file.Path;
import java.util.*;

/**
 * Immutable plugin metadata descriptor.
 *
 * <p>
 * Instances are typically created via {@link Builder} or loaded from a metadata YAML file
 * embedded in a plugin JAR using {@link #createFromJar(File, String)}.
 * </p>
 *
 * <p>
 * Collection fields are stored as immutable snapshots. The class is thread-safe.
 * </p>
 */
public final class Metadata {

    private final String name;
    private final String description;
    private final String id;
    private final String author;
    private final String version;
    private final String license;
    private final String contacts;
    private final File pluginFile;
    private final PluginType type;
    private final Environment environment;
    private final List<String> entryPoints;
    private final List<String> patches;
    private final Map<String, String> dependencies;

    /**
     * Creates a new immutable metadata instance from the given builder.
     *
     * @param builder populated builder instance
     */
    private Metadata(Builder builder) {
        this.name = builder.name;
        this.description = builder.description;
        this.id = builder.id;
        this.author = builder.author;
        this.version = builder.version;
        this.license = builder.license;
        this.contacts = builder.contacts;
        this.pluginFile = builder.pluginFile;
        this.type = builder.type;
        this.environment = builder.environment;

        // Immutable snapshots
        this.entryPoints = List.copyOf(builder.entryPoints);
        this.patches = List.copyOf(builder.patches);
        this.dependencies = Map.copyOf(builder.dependencies);
    }

    /**
     * Loads metadata from a YAML file embedded in the given plugin JAR.
     *
     * <p>
     * If the YAML file does not exist or is empty, {@link Optional#empty()} is returned.
     * </p>
     *
     * @param jarFile          the plugin JAR file; must not be {@code null} and must be a regular file
     * @param metadataFileName the YAML metadata file name inside the JAR; must not be {@code null} or blank
     * @return an optional {@link Metadata} instance
     * @throws NullPointerException     if any argument is {@code null}
     * @throws IllegalArgumentException if {@code jarFile} is not a file or {@code metadataFileName} is blank,
     *                                  or if required YAML fields are missing/invalid
     */
    public static Optional<Metadata> createFromJar(File jarFile, String metadataFileName) {
        Objects.requireNonNull(jarFile, "JAR file must not be null");
        Objects.requireNonNull(metadataFileName, "Metadata file name must not be null");

        if (!jarFile.isFile()) {
            throw new IllegalArgumentException("JAR file must be a regular file: " + jarFile.getAbsolutePath());
        }

        String normalizedMetadataFileName = Builder.normalize(metadataFileName);
        if (normalizedMetadataFileName == null) {
            throw new IllegalArgumentException("Metadata file name must not be blank.");
        }

        YamlFile yamlFile = new YamlFile(jarFile.getAbsolutePath(), normalizedMetadataFileName);
        if (yamlFile.isEmpty()) {
            return Optional.empty();
        }

        Builder builder = new Builder()
                .name(yamlFile.getString("name"))
                .id(yamlFile.getString("id"))
                .description(yamlFile.getString("description"))
                .author(yamlFile.getString("author"))
                .version(yamlFile.getString("version"))
                .license(yamlFile.getString("license"))
                .contacts(yamlFile.getString("contacts"))
                .environment(yamlFile.getString("environment"))
                .type(yamlFile.getString("type"))
                .entryPoints(yamlFile.getStringList("entrypoints"))
                .patches(yamlFile.getStringList("patches"))
                .dependencies(yamlFile.getStringMap("dependencies"))
                .pluginFile(jarFile);

        return Optional.of(builder.build());
    }

    /**
     * @return display name of the plugin
     */
    public String getName() {
        return name;
    }

    /**
     * @return optional description, may be {@code null}
     */
    public String getDescription() {
        return description;
    }

    /**
     * @return stable plugin identifier
     */
    public String getId() {
        return id;
    }

    /**
     * @return plugin author name
     */
    public String getAuthor() {
        return author;
    }

    /**
     * @return plugin version string
     */
    public String getVersion() {
        return version;
    }

    /**
     * @return license name
     */
    public String getLicense() {
        return license;
    }

    /**
     * @return contacts string (URL/email), may be {@code null}
     */
    public String getContacts() {
        return contacts;
    }

    /**
     * @return source JAR file associated with this metadata, may be {@code null}
     */
    public File getPluginFile() {
        return pluginFile;
    }

    /**
     * Returns the plugin file as a {@link Path}.
     *
     * @return optional plugin file path
     */
    public Optional<Path> getPluginPath() {
        return (pluginFile == null) ? Optional.empty() : Optional.of(pluginFile.toPath());
    }

    /**
     * @return plugin type
     */
    public PluginType getType() {
        return type;
    }

    /**
     * @return supported environment
     */
    public Environment getEnvironment() {
        return environment;
    }

    /**
     * Returns plugin entrypoints (e.g. main classes).
     *
     * @return immutable list of entrypoints, never {@code null}
     */
    public List<String> getEntryPoints() {
        return entryPoints;
    }

    /**
     * Returns patch identifiers/classes to apply.
     *
     * @return immutable list of patches, never {@code null}
     */
    public List<String> getPatches() {
        return patches;
    }

    /**
     * Returns plugin dependencies (id -> version range or constraint).
     *
     * @return immutable map of dependencies, never {@code null}
     */
    public Map<String, String> getDependencies() {
        return dependencies;
    }

    @Override
    public String toString() {
        return "Metadata{id='" + id + "', name='" + name + "', version='" + version + "', type=" + type + "}";
    }

    /**
     * Builder for {@link Metadata}.
     *
     * <p>
     * Normalizes string fields by trimming whitespace and converting blanks to {@code null}.
     * Collections are defensively copied and normalized (trimmed, nulls removed).
     * </p>
     */
    public static final class Builder {
        private String name;
        private String description;
        private String id;
        private String author;
        private String version;
        private String license;
        private String contacts;
        private File pluginFile;

        private PluginType type = PluginType.PLUGIN;
        private Environment environment = Environment.BOTH;

        private List<String> entryPoints = List.of();
        private List<String> patches = List.of();
        private Map<String, String> dependencies = Map.of();

        /**
         * @param name plugin display name
         * @return this builder
         */
        public Builder name(String name) {
            this.name = normalize(name);
            return this;
        }

        /**
         * @param description optional plugin description
         * @return this builder
         */
        public Builder description(String description) {
            this.description = normalize(description);
            return this;
        }

        /**
         * @param id stable plugin identifier
         * @return this builder
         */
        public Builder id(String id) {
            this.id = normalize(id);
            return this;
        }

        /**
         * @param author plugin author
         * @return this builder
         */
        public Builder author(String author) {
            this.author = normalize(author);
            return this;
        }

        /**
         * @param version plugin version string
         * @return this builder
         */
        public Builder version(String version) {
            this.version = normalize(version);
            return this;
        }

        /**
         * @param license license name
         * @return this builder
         */
        public Builder license(String license) {
            this.license = normalize(license);
            return this;
        }

        /**
         * @param contacts contacts string (URL/email)
         * @return this builder
         */
        public Builder contacts(String contacts) {
            this.contacts = normalize(contacts);
            return this;
        }

        /**
         * @param pluginFile plugin source JAR file
         * @return this builder
         */
        public Builder pluginFile(File pluginFile) {
            this.pluginFile = pluginFile;
            return this;
        }

        /**
         * @param environment environment as string (e.g. "CLIENT", "SERVER", "BOTH")
         * @return this builder
         */
        public Builder environment(String environment) {
            String normalized = normalize(environment);
            if (normalized != null) {
                this.environment = Environment.fromString(normalized);
            }
            return this;
        }

        /**
         * @param type plugin type as string (e.g. "PLUGIN", "LOADER", "PROVIDER")
         * @return this builder
         */
        public Builder type(String type) {
            if (type == null) {
                this.type = PluginType.PLUGIN;
            }
            
            String normalized = normalize(type);
            if (normalized != null) {
                this.type = PluginType.fromValue(normalized);
            }

            return this;
        }

        /**
         * @param type plugin type enum
         * @return this builder
         * @throws NullPointerException if {@code type} is {@code null}
         */
        public Builder type(PluginType type) {
            this.type = Objects.requireNonNull(type, "Plugin type must not be null");
            return this;
        }

        /**
         * @param entryPoints list of entrypoints
         * @return this builder
         */
        public Builder entryPoints(List<String> entryPoints) {
            this.entryPoints = normalizeStringList(entryPoints);
            return this;
        }

        /**
         * @param patches list of patches
         * @return this builder
         */
        public Builder patches(List<String> patches) {
            this.patches = normalizeStringList(patches);
            return this;
        }

        /**
         * @param dependencies dependency map (id -> version constraint)
         * @return this builder
         */
        public Builder dependencies(Map<String, String> dependencies) {
            this.dependencies = normalizeStringMap(dependencies);
            return this;
        }

        /**
         * Builds an immutable {@link Metadata} instance.
         *
         * @return metadata instance
         * @throws IllegalArgumentException if required fields are missing/invalid
         */
        public Metadata build() {
            validate();
            return new Metadata(this);
        }

        /**
         * Validates builder fields according to the metadata contract.
         *
         * <p>
         * Required fields: {@code name}, {@code id}, {@code author}, {@code version}, {@code license}.
         * </p>
         *
         * <p>
         * Entrypoints are required for {@link PluginType#PLUGIN} and {@link PluginType#PROVIDER}.
         * Loader metadata may have an empty entrypoints list.
         * </p>
         *
         * @throws IllegalArgumentException if validation fails
         */
        private void validate() {
            requireNotBlank(name, "name");
            id = normalizeId(id);
            requireNotBlank(id, "id");
            requireNotBlank(author, "author");
            requireNotBlank(version, "version");
            requireNotBlank(license, "license");
            Objects.requireNonNull(type, "type must not be null");
            Objects.requireNonNull(environment, "environment must not be null");
            Objects.requireNonNull(entryPoints, "entryPoints must not be null");

            // Entry points are required only for regular plugins and providers.
            if ((type == PluginType.PLUGIN || type == PluginType.PROVIDER) && entryPoints.isEmpty()) {
                throw new IllegalArgumentException("The required field 'entrypoints' is empty in the metadata.");
            }
        }

        /**
         * Normalizes id to match the format "text-text-text"
         * (i.e. alphanumeric lines separated by hyphens).
         *
         * @param id String id to be normalized
         * @return Normalized id string
         */
        private String normalizeId(String id) {
            if (id == null) {
                return null;
            }

            id = id.trim();

            // Check for validity: only letters, numbers and hyphens.
            if (!id.matches("[a-zA-Z0-9-]+")) {
                throw new IllegalArgumentException("ID can only contain letters, digits, and hyphens.");
            }

            // Convert the ID to lowercase and replace several hyphens with one.
            id = id.toLowerCase().replaceAll("-+", "-");

            // Remove hyphens at the beginning and end of the line
            if (id.startsWith("-")) {
                id = id.substring(1);
            }
            if (id.endsWith("-")) {
                id = id.substring(0, id.length() - 1);
            }

            return id;
        }

        /**
         * Normalizes an input string by trimming whitespace and converting blank strings to {@code null}.
         *
         * @param text input text
         * @return normalized string or {@code null} if the input is {@code null} or blank
         */
        static String normalize(String text) {
            if (text == null) {
                return null;
            }
            String trimmed = text.trim();
            return trimmed.isEmpty() ? null : trimmed;
        }

        /**
         * Ensures that a required string value is not {@code null} and not blank.
         *
         * @param value     the value to check
         * @param fieldName the logical field name used for error messages
         * @throws IllegalArgumentException if {@code value} is {@code null} or blank
         */
        private static void requireNotBlank(String value, String fieldName) {
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException(
                        "The required field '" + fieldName + "' is not specified in the metadata."
                );
            }
        }

        /**
         * Normalizes a list of strings by:
         * <ul>
         *   <li>treating {@code null} as an empty list</li>
         *   <li>trimming each element</li>
         *   <li>removing {@code null} and blank elements</li>
         * </ul>
         *
         * @param values input list
         * @return a mutable normalized list or an immutable empty list if no values remain
         */
        private static List<String> normalizeStringList(List<String> values) {
            if (values == null || values.isEmpty()) {
                return List.of();
            }
            ArrayList<String> result = new ArrayList<>(values.size());
            for (String value : values) {
                String normalized = normalize(value);
                if (normalized != null) {
                    result.add(normalized);
                }
            }
            return result.isEmpty() ? List.of() : result;
        }

        /**
         * Normalizes a map of strings by:
         * <ul>
         *   <li>treating {@code null} as an empty map</li>
         *   <li>trimming keys and values</li>
         *   <li>dropping entries with {@code null} or blank keys</li>
         *   <li>converting {@code null} or blank values to an empty string</li>
         * </ul>
         *
         * @param values input map
         * @return a mutable normalized map or an immutable empty map if no entries remain
         */
        private static Map<String, String> normalizeStringMap(Map<String, String> values) {
            if (values == null || values.isEmpty()) {
                return Map.of();
            }
            HashMap<String, String> result = new HashMap<>(Math.max(16, values.size()));
            for (Map.Entry<String, String> entry : values.entrySet()) {
                String key = normalize(entry.getKey());
                if (key == null) {
                    continue;
                }
                String value = normalize(entry.getValue());
                result.put(key, (value == null) ? "" : value);
            }
            return result.isEmpty() ? Map.of() : result;
        }
    }
}