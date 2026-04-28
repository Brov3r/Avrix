package com.avrix.core;

import com.avrix.provider.GameProvider;
import com.avrix.utils.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Immutable metadata descriptor for an Avrix plugin.
 * Contains all declarative information parsed from a plugin's manifest file.
 * Used by the framework for validation, discovery, and runtime wiring.
 */
public class Metadata {
    private static final Logger log = LoggerFactory.getLogger(Metadata.class);

    /**
     * Schema version of the metadata format.
     */
    private final int schema;

    /**
     * Human-readable plugin name.
     */
    private final String name;

    /**
     * Short plugin description.
     */
    private final String description;

    /**
     * Unique plugin identifier (e.g., {@code plugin-id}).
     */
    private final String id;

    /**
     * Plugin version string (SemVer recommended).
     */
    private final String version;

    /**
     * Target {@link Environment}
     */
    private final Environment environment;

    /**
     * List of author names.
     */
    private final List<String> authors;

    /**
     * License identifier (e.g., {@code "MIT"}, {@code "Apache-2.0"}).
     */
    private final String license;

    /**
     * Contact information (emails, URLs).
     */
    private final List<String> contacts;

    /**
     * Dependencies map: {@code pluginId -> versionConstraint}.
     */
    private final Map<String, String> dependencies;

    /**
     * Fully-qualified class name of the plugin entrypoint.
     */
    private final String entrypoint;

    /**
     * List of Mixin configuration fully-qualified class name.
     */
    private final List<String> mixins;

    /**
     * Physical plugin file (JAR/directory)
     */
    private File pluginFile;

    /**
     * Optional plugin icon url (path to the icon, including the plugin's JAR file)
     */
    private URL iconUrl;

    /**
     * Creates a new Metadata instance.
     *
     * @param schema       metadata schema version
     * @param name         human-readable plugin name
     * @param description  short plugin description
     * @param id           unique plugin identifier
     * @param version      plugin version string
     * @param environment  target runtime environment
     * @param authors      list of author names
     * @param license      license identifier
     * @param contacts     list of contact strings
     * @param dependencies map of plugin dependencies
     * @param entrypoint   fully-qualified entrypoint class name
     * @param mixins       list of Mixin config paths
     */
    public Metadata(int schema, String name, String description, String id, String version,
                    Environment environment, List<String> authors, String license, List<String> contacts,
                    Map<String, String> dependencies, String entrypoint, List<String> mixins, File pluginFile, URL iconUrl) {
        this.schema = schema;
        this.name = name;
        this.description = description;
        this.id = id;
        this.version = version;
        this.environment = environment;
        this.authors = authors;
        this.license = license;
        this.contacts = contacts;
        this.dependencies = dependencies;
        this.entrypoint = entrypoint;
        this.mixins = mixins;
        this.pluginFile = pluginFile;
        this.iconUrl = iconUrl;
    }

    /**
     * Returns the metadata schema version.
     *
     * @return schema version number
     */
    public int getSchema() {
        return schema;
    }

    /**
     * Returns the human-readable plugin name.
     *
     * @return plugin name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the short plugin description.
     *
     * @return description text
     */
    public String getDescription() {
        return description;
    }

    /**
     * Returns the unique plugin identifier.
     *
     * @return plugin ID (e.g., {@code plugin-id})
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the plugin version string.
     *
     * @return version (e.g., {@code "1.2.3"})
     */
    public String getVersion() {
        return version;
    }

    /**
     * Returns the target {@link Environment}
     *
     * @return {@link Environment}
     */
    public Environment getEnvironment() {
        return environment;
    }

    /**
     * Returns the list of author names.
     *
     * @return immutable list of authors
     */
    public List<String> getAuthors() {
        return authors;
    }

    /**
     * Returns the license identifier.
     *
     * @return license name (e.g., {@code "MIT"})
     */
    public String getLicense() {
        return license;
    }

    /**
     * Returns the list of contact strings.
     *
     * @return immutable list of contacts
     */
    public List<String> getContacts() {
        return contacts;
    }

    /**
     * Returns the dependencies map.
     *
     * @return map of {@code pluginId -> versionConstraint}
     */
    public Map<String, String> getDependencies() {
        return dependencies;
    }

    /**
     * Returns the fully-qualified entrypoint class name.
     *
     * @return entrypoint class name
     */
    public String getEntrypoint() {
        return entrypoint;
    }

    /**
     * Returns the list of Mixin fully-qualified entrypoint class name.s.
     *
     * @return immutable list of Mixin fully-qualified entrypoint class name.
     */
    public List<String> getMixins() {
        return mixins;
    }

    /**
     * Returns the physical plugin file reference.
     *
     * @return plugin {@link File}, or {@code null} if not set
     */
    public File getPluginFile() {
        return pluginFile;
    }

    /**
     * Returns the optional plugin icon url reference (path to the icon, including the plugin's JAR file)
     *
     * @return icon {@link URL}, or {@code null} if not set
     */
    public URL getIconFileURL() {
        return iconUrl;
    }

    /**
     * Creates metadata for the Avrix loader itself.
     * Initializes all fields using predefined framework constants.
     *
     * @return loader {@link Metadata} instance
     */
    public static Metadata getLoaderMetadata() {
        return new Builder()
                .schema(Constants.METADATA_SCHEMA)
                .authors(Constants.LOADER_AUTHOR)
                .name(Constants.LOADER_NAME)
                .id(Constants.LOADER_ID)
                .license(Constants.LOADER_LICENSE)
                .contacts(Constants.LOADER_CONTACTS)
                .version(Constants.LOADER_VERSION)
                .environment(Environment.BOTH)
                .build();
    }

    /**
     * Creates metadata from an existing {@link GameProvider} instance.
     * Maps provider properties to the corresponding metadata fields.
     *
     * @param provider the source game provider
     * @return configured {@link Metadata} instance
     * @throws RuntimeException if required provider fields are null or blank
     */
    public static Metadata fromGameProvider(GameProvider provider) {
        return new Builder()
                .schema(Constants.METADATA_SCHEMA)
                .authors(provider.getAuthors())
                .name(provider.getName())
                .id(provider.getId())
                .license(provider.getLicense())
                .contacts(provider.getContacts())
                .version(provider.getNormalizedVersion())
                .environment(provider.getEnvironment())
                .entrypoint(provider.getEntrypoint())
                .build();
    }

    /**
     * Loads plugin {@link Metadata} from a YAML entry inside a JAR file.
     *
     * @param jarFile   path to the plugin JAR
     * @param entryPath path to the metadata YAML inside the JAR
     * @return parsed {@link Metadata} instance with resolved file paths
     * @throws IOException           if reading the JAR or YAML fails
     * @throws FileNotFoundException if the specified entry does not exist
     */
    public static Metadata fromJarFile(File jarFile, String entryPath) throws IOException {
        log.debug("Loading metadata from JAR '{}' (entry: {})", jarFile, entryPath);
        try (JarFile jar = new JarFile(jarFile)) {
            JarEntry entry = jar.getJarEntry(entryPath);
            if (entry == null) {
                throw new FileNotFoundException("Metadata entry not found in JAR: " + entryPath);
            }

            try (InputStream in = jar.getInputStream(entry)) {
                YamlConfigurationLoader loader = YamlConfigurationLoader.builder()
                        .source(() -> new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)))
                        .build();
                Metadata jarMeta = fromNode(loader.load());
                jarMeta.pluginFile = jarFile;

                try {
                    jarMeta.iconUrl = new URI("jar:" + jarFile.toURI() + "!/" + Constants.PLUGINS_ICON_NAME).toURL();
                } catch (URISyntaxException e) {
                    throw new IOException("Invalid icon URI for " + jarFile.getName(), e);
                }

                log.info("Metadata loaded for plugin '{}' from {}", jarMeta.getId(), jarFile.getName());
                return jarMeta;
            }
        } catch (FileNotFoundException e) {
            log.warn("{}", e.getMessage());
            throw e;
        } catch (IOException e) {
            log.error("Failed to load metadata from {}: {}", jarFile.getName(), e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error parsing metadata from {}: {}", jarFile.getName(), e.getMessage(), e);
            throw new IOException("Failed to parse metadata", e);
        }
    }

    /**
     * Loads {@link Metadata} from a YAML file at the given path.
     *
     * @param path path to the YAML file
     * @return parsed {@link Metadata}
     * @throws IOException if the file cannot be read
     */
    public static Metadata fromYaml(Path path) throws IOException {
        YamlConfigurationLoader loader = YamlConfigurationLoader.builder().path(path).build();
        return fromNode(loader.load());
    }

    /**
     * Parses {@link Metadata} from a Configurate {@link ConfigurationNode}.
     *
     * @param node root {@link ConfigurationNode}
     * @return parsed {@link Metadata}
     * @throws SerializationException if type conversion fails
     */
    public static Metadata fromNode(ConfigurationNode node) throws SerializationException {
        Metadata.Builder builder = new Builder()
                .schema(node.node("schema").getInt(0))
                .name(node.node("name").getString())
                .description(node.node("description").getString())
                .id(node.node("id").getString())
                .version(node.node("version").getString())
                .entrypoint(node.node("entrypoint").getString())
                .license(node.node("license").getString());

        if (!node.node("authors").virtual())
            builder.authors(node.node("authors").getList(String.class));
        if (!node.node("contacts").virtual())
            builder.contacts(node.node("contacts").getList(String.class));
        if (!node.node("environment").virtual()) {
            String env = node.node("environment").getString("both");
            if ("*".equals(env)) env = "both";
            builder.environment(Environment.fromString(env.toLowerCase(Locale.ROOT)));
        }

        if (!node.node("mixins").virtual())
            builder.mixins(node.node("mixins").getList(String.class));

        ConfigurationNode depsNode = node.node("dependencies");
        if (!depsNode.virtual() && depsNode.isMap()) {
            for (var entry : depsNode.childrenMap().entrySet()) {
                builder.addDependency(entry.getKey().toString(), entry.getValue().getString());
            }
        }

        return builder.build();
    }

    /**
     * Class for creating new metadata for the "Builder" patern
     */
    public static class Builder {
        /**
         * Schema version of the metadata format.
         */
        private int schema;

        /**
         * Human-readable plugin name.
         */
        private String name;

        /**
         * Short plugin description.
         */
        private String description;

        /**
         * Unique plugin identifier (e.g., {@code plugin-id}).
         */
        private String id;

        /**
         * Plugin version string (SemVer recommended).
         */
        private String version;

        /**
         * Target {@link Environment}
         */
        private Environment environment;

        /**
         * List of author names.
         */
        private List<String> authors = new ArrayList<>();

        /**
         * License identifier (e.g., {@code "MIT"}, {@code "Apache-2.0"}).
         */
        private String license;

        /**
         * Contact information (emails, URLs).
         */
        private List<String> contacts = new ArrayList<>();

        /**
         * Dependencies map: {@code pluginId -> versionConstraint}.
         */
        private Map<String, String> dependencies = new HashMap<>();

        /**
         * Fully-qualified class name of the plugin entrypoint.
         */
        private String entrypoint;

        /**
         * List of Mixin configuration fully-qualified class name.
         */
        private List<String> mixins = new ArrayList<>();

        /**
         * Physical plugin file (JAR/directory)
         */
        private File pluginFile;

        /**
         * Optional plugin icon url
         */
        private URL iconUrl;

        /**
         * Sets the metadata schema version.
         *
         * @param schema schema version number
         * @return this builder for method chaining
         */
        public Metadata.Builder schema(int schema) {
            this.schema = schema;
            return this;
        }

        /**
         * Sets the human-readable plugin name.
         *
         * @param name plugin name
         * @return this builder for method chaining
         */
        public Metadata.Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Sets the short plugin description.
         *
         * @param description description text
         * @return this builder for method chaining
         */
        public Metadata.Builder description(String description) {
            this.description = description;
            return this;
        }

        /**
         * Sets the unique plugin identifier.
         *
         * @param id plugin ID
         * @return this builder for method chaining
         */
        public Metadata.Builder id(String id) {
            this.id = id;
            return this;
        }

        /**
         * Sets the plugin version string.
         *
         * @param version version string
         * @return this builder for method chaining
         */
        public Metadata.Builder version(String version) {
            this.version = version;
            return this;
        }

        /**
         * Sets the target runtime {@link Environment}. Defaults to {@link Environment#BOTH} if null.
         *
         * @param environment target {@link Environment}
         * @return this builder for method chaining
         */
        public Metadata.Builder environment(Environment environment) {
            this.environment = environment != null ? environment : Environment.BOTH;
            return this;
        }

        /**
         * Sets the list of plugin authors. Replaces existing list with a copy.
         *
         * @param authors list of author names, or null to clear
         * @return this builder for method chaining
         */
        public Metadata.Builder authors(List<String> authors) {
            this.authors = authors != null ? new ArrayList<>(authors) : new ArrayList<>();
            return this;
        }

        /**
         * Sets the plugin authors from a varargs array.
         *
         * @param authors author names
         * @return this builder for method chaining
         */
        public Metadata.Builder authors(String... authors) {
            this.authors = new ArrayList<>(Arrays.asList(authors));
            return this;
        }

        /**
         * Adds a single author to the list.
         *
         * @param author author name
         * @return this builder for method chaining
         */
        public Metadata.Builder addAuthor(String author) {
            this.authors.add(author);
            return this;
        }

        /**
         * Sets the plugin license identifier.
         *
         * @param license license name (e.g., "MIT")
         * @return this builder for method chaining
         */
        public Metadata.Builder license(String license) {
            this.license = license;
            return this;
        }

        /**
         * Sets the list of contact strings. Replaces existing list with a copy.
         *
         * @param contacts list of contacts, or null to clear
         * @return this builder for method chaining
         */
        public Metadata.Builder contacts(List<String> contacts) {
            this.contacts = contacts != null ? new ArrayList<>(contacts) : new ArrayList<>();
            return this;
        }

        /**
         * Sets the plugin contacts from a varargs array.
         *
         * @param contacts contact strings
         * @return this builder for method chaining
         */
        public Metadata.Builder contacts(String... contacts) {
            this.contacts = new ArrayList<>(Arrays.asList(contacts));
            return this;
        }

        /**
         * Adds a single contact string to the list.
         *
         * @param contact contact information
         * @return this builder for method chaining
         */
        public Metadata.Builder addContact(String contact) {
            this.contacts.add(contact);
            return this;
        }

        /**
         * Sets the dependencies map. Replaces existing map with a copy.
         *
         * @param map map of pluginId -> versionConstraint, or null to clear
         * @return this builder for method chaining
         */
        public Metadata.Builder dependencies(Map<String, String> map) {
            this.dependencies = map != null ? new HashMap<>(map) : new HashMap<>();
            return this;
        }

        /**
         * Adds a single dependency to the map.
         *
         * @param id         required plugin ID
         * @param constrains version constraint string
         * @return this builder for method chaining
         */
        public Metadata.Builder addDependency(String id, String constrains) {
            this.dependencies.put(id, constrains);
            return this;
        }

        /**
         * Sets the fully-qualified entrypoint class name.
         *
         * @param entrypoint entrypoint class name
         * @return this builder for method chaining
         */
        public Metadata.Builder entrypoint(String entrypoint) {
            this.entrypoint = entrypoint;
            return this;
        }

        /**
         * Sets the list of Mixin configuration paths. Replaces existing list with a copy.
         *
         * @param mixins list of Mixin file paths, or null to clear
         * @return this builder for method chaining
         */
        public Metadata.Builder mixins(List<String> mixins) {
            this.mixins = mixins != null ? new ArrayList<>(mixins) : new ArrayList<>();
            return this;
        }

        /**
         * Sets the Mixin configurations from a varargs array.
         *
         * @param mixins Mixin configuration paths
         * @return this builder for method chaining
         */
        public Metadata.Builder mixins(String... mixins) {
            this.mixins = new ArrayList<>(Arrays.asList(mixins));
            return this;
        }

        /**
         * Adds a single Mixin configuration path to the list.
         *
         * @param mixin Mixin config path
         * @return this builder for method chaining
         */
        public Metadata.Builder addMixin(String mixin) {
            this.mixins.add(mixin);
            return this;
        }

        /**
         * Sets the physical plugin file reference.
         *
         * @param file plugin {@link File}
         * @return this builder for method chaining
         */
        public Metadata.Builder pluginFile(File file) {
            this.pluginFile = file;
            return this;
        }

        /**
         * Sets the optional plugin icon url reference (path to the icon, including the plugin's JAR file).
         *
         * @param url icon {@link URL}
         * @return this builder for method chaining
         */
        public Metadata.Builder iconUrl(URL url) {
            this.iconUrl = url;
            return this;
        }

        /**
         * Validates required fields and constructs an immutable {@link Metadata} instance.
         *
         * @return the fully initialized {@link Metadata} object
         * @throws RuntimeException if the schema version is unsupported, or if {@code id},
         *                          {@code name}, or {@code version} is null or blank
         */
        public Metadata build() {
            if (schema != Constants.METADATA_SCHEMA) {
                throw new RuntimeException(String.format(
                        "Unsupported metadata schema version (metadata: %d, supported: %d)",
                        schema, Constants.METADATA_SCHEMA));
            }
            if (id == null || id.isBlank()) {
                throw new RuntimeException("The 'id' cannot be null or empty!");
            }
            if (name == null || name.isBlank()) {
                throw new RuntimeException("The 'name' cannot be null or empty!");
            }
            if (version == null || version.isBlank()) {
                throw new RuntimeException("The 'version' cannot be null or empty!");
            }

            return new Metadata(schema, name, description, id, version, environment, authors, license,
                    contacts, dependencies, entrypoint, mixins, pluginFile, iconUrl);
        }
    }
}
