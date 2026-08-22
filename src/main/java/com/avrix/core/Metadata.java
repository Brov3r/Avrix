package com.avrix.core;

import com.avrix.provider.GameProvider;
import com.avrix.utils.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Immutable metadata descriptor for an Avrix plugin or runtime game provider.
 * <p>
 * Encapsulates all declarative manifest information parsed from {@code plugin.yaml} or synthesized
 * from active game runtime providers. Used for topological dependency sorting, environment filtering,
 * and mixin bootstrap configuration.
 *
 * @param schema       metadata specification schema version (mandatory, must match {@link Constants#METADATA_SCHEMA})
 * @param name         human-readable display name of the plugin (mandatory, non-blank)
 * @param description  brief summary of plugin functionality; defaults to empty string
 * @param id           unique alphanumeric identifier (mandatory, non-blank)
 * @param version      semantic version string conforming to SemVer (mandatory, non-blank)
 * @param environment  target execution environment constraint; defaults to {@link Environment#BOTH}
 * @param authors      unmodifiable list of author attribution names
 * @param license      software licensing model descriptor; defaults to {@code "UNLICENSED"}
 * @param contacts     unmodifiable list of contact endpoints or repository links
 * @param dependencies unmodifiable map of prerequisite plugin IDs to semantic version constraint ranges
 * @param loadBefore   unmodifiable list of plugin IDs that must be loaded strictly after this plugin
 * @param loadAfter    unmodifiable list of plugin IDs that must be loaded strictly before this plugin
 * @param entrypoint   fully-qualified class name of the plugin entrypoint class; defaults to empty string
 * @param mixins       unmodifiable list of mixin configuration paths inside the plugin JAR
 */
public record Metadata(
        int schema,
        String name,
        String description,
        String id,
        String version,
        Environment environment,
        List<String> authors,
        String license,
        List<String> contacts,
        Map<String, String> dependencies,
        List<String> loadBefore,
        List<String> loadAfter,
        String entrypoint,
        List<String> mixins
) {
    private static final Logger LOGGER = LoggerFactory.getLogger(Metadata.class);

    /**
     * Compact constructor enforcing domain invariants, mandatory attribute validation, and collection immutability.
     *
     * @throws NullPointerException     if any mandatory string field ({@code id}, {@code name}, {@code version}) is {@code null}
     * @throws IllegalArgumentException if string invariants (non-blank) or supported schema versions are violated
     */
    public Metadata {
        // Mandatory schema verification
        if (schema != Constants.METADATA_SCHEMA) {
            throw new IllegalArgumentException(
                    "Unsupported metadata schema version (metadata: %d, supported: %d)".formatted(schema, Constants.METADATA_SCHEMA)
            );
        }

        // Mandatory string attributes verification
        Objects.requireNonNull(id, "Plugin 'id' cannot be null");
        Objects.requireNonNull(name, "Plugin 'name' cannot be null");
        Objects.requireNonNull(version, "Plugin 'version' cannot be null");

        if (id.isBlank()) {
            throw new IllegalArgumentException("Plugin 'id' cannot be blank");
        }
        if (name.isBlank()) {
            throw new IllegalArgumentException("Plugin 'name' cannot be blank");
        }
        if (version.isBlank()) {
            throw new IllegalArgumentException("Plugin 'version' cannot be blank");
        }

        // Optional scalar fields normalization
        environment = Objects.requireNonNullElse(environment, Environment.BOTH);
        description = Objects.requireNonNullElse(description, "");
        license = Objects.requireNonNullElse(license, "UNLICENSED");
        entrypoint = Objects.requireNonNullElse(entrypoint, "");

        // Defensive copy and unmodifiable collection creation
        authors = (authors == null) ? List.of() : List.copyOf(authors);
        contacts = (contacts == null) ? List.of() : List.copyOf(contacts);
        mixins = (mixins == null) ? List.of() : List.copyOf(mixins);
        dependencies = (dependencies == null) ? Map.of() : Map.copyOf(dependencies);
        loadBefore = (loadBefore == null) ? List.of() : List.copyOf(loadBefore);
        loadAfter = (loadAfter == null) ? List.of() : List.copyOf(loadAfter);
    }

    /**
     * Synthesizes a {@link Metadata} descriptor representing the underlying game from a {@link GameProvider}.
     *
     * @param provider the active game provider instance
     * @return synthesized game metadata
     * @throws NullPointerException if {@code provider} is {@code null}
     */
    public static Metadata fromGameProvider(GameProvider provider) {
        Objects.requireNonNull(provider, "GameProvider cannot be null");
        return new Builder()
                .schema(Constants.METADATA_SCHEMA)
                .id(provider.getId())
                .name(provider.getName())
                .version(provider.getNormalizedVersion())
                .environment(provider.getEnvironment())
                .authors(provider.getAuthors())
                .license(provider.getLicense())
                .contacts(provider.getContacts())
                .entrypoint(provider.getEntrypoint())
                .build();
    }

    /**
     * Parses a plugin metadata manifest located inside a JAR file.
     *
     * @param jarPath   the path to the physical JAR file on disk
     * @param entryPath internal relative path to the YAML manifest (e.g., {@code "plugin.yaml"})
     * @return parsed immutable metadata
     * @throws NullPointerException  if {@code jarPath} or {@code entryPath} is {@code null}
     * @throws FileNotFoundException if the specified entry is missing inside the JAR
     * @throws IOException           if an I/O error occurs while reading the JAR or parsing YAML
     */
    public static Metadata fromJarFile(Path jarPath, String entryPath) throws IOException {
        Objects.requireNonNull(jarPath, "JAR path cannot be null");
        Objects.requireNonNull(entryPath, "Entry path cannot be null");

        LOGGER.debug("Loading plugin metadata from JAR [{}] at entry [{}]", jarPath, entryPath);
        try (JarFile jar = new JarFile(jarPath.toFile())) {
            JarEntry entry = jar.getJarEntry(entryPath);
            if (entry == null) {
                throw new FileNotFoundException("Metadata entry [%s] not found in JAR [%s]".formatted(entryPath, jarPath));
            }

            try (InputStream in = jar.getInputStream(entry)) {
                YamlConfigurationLoader loader = YamlConfigurationLoader.builder()
                        .source(() -> new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)))
                        .build();

                Metadata metadata = fromNode(loader.load());
                LOGGER.debug("Successfully loaded metadata for plugin [{}] (v{})", metadata.id(), metadata.version());
                return metadata;
            }
        } catch (FileNotFoundException e) {
            LOGGER.warn("Metadata manifest not found: {}", e.getMessage());
            throw e;
        } catch (IOException e) {
            LOGGER.error("Failed to parse metadata manifest from JAR [{}]: {}", jarPath.getFileName(), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Parses a plugin metadata manifest from a {@link File} reference.
     *
     * @param jarFile   the physical JAR file
     * @param entryPath internal relative path to the YAML manifest
     * @return parsed immutable metadata
     * @throws NullPointerException if {@code jarFile} or {@code entryPath} is {@code null}
     * @throws IOException          if reading the manifest fails
     */
    public static Metadata fromJarFile(File jarFile, String entryPath) throws IOException {
        Objects.requireNonNull(jarFile, "JarFile cannot be null");
        return fromJarFile(jarFile.toPath(), entryPath);
    }

    /**
     * Parses metadata from an external YAML file on the filesystem.
     *
     * @param path path to the YAML file
     * @return parsed immutable metadata
     * @throws NullPointerException if {@code path} is {@code null}
     * @throws IOException          if reading fails or YAML structure is invalid
     */
    public static Metadata fromYaml(Path path) throws IOException {
        Objects.requireNonNull(path, "Path cannot be null");
        YamlConfigurationLoader loader = YamlConfigurationLoader.builder().path(path).build();
        return fromNode(loader.load());
    }

    /**
     * Deserializes a {@link Metadata} record from a Configurate {@link ConfigurationNode}.
     *
     * @param node the root configuration node
     * @return populated {@link Metadata}
     * @throws NullPointerException   if {@code node} is {@code null}
     * @throws SerializationException if required nodes cannot be deserialized
     */
    public static Metadata fromNode(ConfigurationNode node) throws SerializationException {
        Objects.requireNonNull(node, "ConfigurationNode cannot be null");

        Builder builder = new Builder()
                .schema(node.node("schema").getInt(Constants.METADATA_SCHEMA))
                .id(node.node("id").getString())
                .name(node.node("name").getString())
                .description(node.node("description").getString(""))
                .version(node.node("version").getString())
                .entrypoint(node.node("entrypoint").getString(""))
                .license(node.node("license").getString("UNLICENSED"));

        if (!node.node("environment").virtual()) {
            builder.environment(Environment.fromString(node.node("environment").getString("both")));
        }

        if (!node.node("loadBefore").virtual()) {
            builder.loadBefore(node.node("loadBefore").getList(String.class, Collections.emptyList()));
        }

        if (!node.node("loadAfter").virtual()) {
            builder.loadAfter(node.node("loadAfter").getList(String.class, Collections.emptyList()));
        }

        if (!node.node("authors").virtual()) {
            builder.authors(node.node("authors").getList(String.class, Collections.emptyList()));
        }

        if (!node.node("contacts").virtual()) {
            builder.contacts(node.node("contacts").getList(String.class, Collections.emptyList()));
        }

        if (!node.node("mixins").virtual()) {
            builder.mixins(node.node("mixins").getList(String.class, Collections.emptyList()));
        }

        ConfigurationNode dependenciesNode = node.node("dependencies");
        if (!dependenciesNode.virtual() && dependenciesNode.isMap()) {
            for (var entry : dependenciesNode.childrenMap().entrySet()) {
                String depId = String.valueOf(entry.getKey());
                String versionConstraint = entry.getValue().getString("*");
                builder.addDependency(depId, versionConstraint);
            }
        }

        return builder.build();
    }

    /**
     * Fluent Builder for constructing immutable {@link Metadata} instances.
     */
    public static final class Builder {

        private int schema = Constants.METADATA_SCHEMA;
        private String name;
        private String description = "";
        private String id;
        private String version;
        private Environment environment = Environment.BOTH;
        private final List<String> authors = new ArrayList<>();
        private String license = "UNLICENSED";
        private final List<String> contacts = new ArrayList<>();
        private final Map<String, String> dependencies = new HashMap<>();
        private final List<String> loadBefore = new ArrayList<>();
        private final List<String> loadAfter = new ArrayList<>();
        private String entrypoint = "";
        private final List<String> mixins = new ArrayList<>();

        /**
         * Sets the schema version.
         *
         * @param schema specification version number
         * @return this builder instance
         */
        public Builder schema(int schema) {
            this.schema = schema;
            return this;
        }

        /**
         * Sets the human-readable display name.
         *
         * @param name plugin display name
         * @return this builder instance
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Sets the plugin description.
         *
         * @param description summary description
         * @return this builder instance
         */
        public Builder description(String description) {
            this.description = description;
            return this;
        }

        /**
         * Sets the unique plugin identifier.
         *
         * @param id plugin identifier
         * @return this builder instance
         */
        public Builder id(String id) {
            this.id = id;
            return this;
        }

        /**
         * Sets the semantic version string.
         *
         * @param version semantic version string
         * @return this builder instance
         */
        public Builder version(String version) {
            this.version = version;
            return this;
        }

        /**
         * Sets the execution environment constraint.
         *
         * @param environment target environment constraint
         * @return this builder instance
         */
        public Builder environment(Environment environment) {
            this.environment = Objects.requireNonNullElse(environment, Environment.BOTH);
            return this;
        }

        /**
         * Appends a list of author names.
         *
         * @param authors list of authors
         * @return this builder instance
         */
        public Builder authors(List<String> authors) {
            if (authors != null) {
                this.authors.addAll(authors);
            }
            return this;
        }

        /**
         * Appends multiple author names.
         *
         * @param authors array of author names
         * @return this builder instance
         */
        public Builder authors(String... authors) {
            if (authors != null) {
                this.authors.addAll(Arrays.asList(authors));
            }
            return this;
        }

        /**
         * Appends a single author name.
         *
         * @param author author name
         * @return this builder instance
         */
        public Builder addAuthor(String author) {
            if (author != null && !author.isBlank()) {
                this.authors.add(author);
            }
            return this;
        }

        /**
         * Sets the license identifier.
         *
         * @param license license descriptor
         * @return this builder instance
         */
        public Builder license(String license) {
            this.license = license;
            return this;
        }

        /**
         * Appends a list of contact endpoints.
         *
         * @param contacts list of contact URLs or strings
         * @return this builder instance
         */
        public Builder contacts(List<String> contacts) {
            if (contacts != null) {
                this.contacts.addAll(contacts);
            }
            return this;
        }

        /**
         * Appends multiple contact endpoints.
         *
         * @param contacts array of contact strings
         * @return this builder instance
         */
        public Builder contacts(String... contacts) {
            if (contacts != null) {
                this.contacts.addAll(Arrays.asList(contacts));
            }
            return this;
        }

        /**
         * Appends a single contact endpoint.
         *
         * @param contact contact URL or handle
         * @return this builder instance
         */
        public Builder addContact(String contact) {
            if (contact != null && !contact.isBlank()) {
                this.contacts.add(contact);
            }
            return this;
        }

        /**
         * Populates the dependencies map.
         *
         * @param dependencies map of dependency plugin IDs to SemVer constraint ranges
         * @return this builder instance
         */
        public Builder dependencies(Map<String, String> dependencies) {
            if (dependencies != null) {
                this.dependencies.putAll(dependencies);
            }
            return this;
        }

        /**
         * Appends a single plugin dependency constraint.
         *
         * @param id                target plugin ID
         * @param versionConstraint SemVer constraint expression (e.g., {@code ">=1.0.0"})
         * @return this builder instance
         */
        public Builder addDependency(String id, String versionConstraint) {
            if (id != null && !id.isBlank() && versionConstraint != null && !versionConstraint.isBlank()) {
                this.dependencies.put(id, versionConstraint);
            }
            return this;
        }

        /**
         * Appends a list of plugin IDs that must load after this plugin.
         *
         * @param ids list of target plugin IDs
         * @return this builder instance
         */
        public Builder loadBefore(List<String> ids) {
            if (ids != null) {
                this.loadBefore.addAll(ids);
            }
            return this;
        }

        /**
         * Appends multiple plugin IDs that must load after this plugin.
         *
         * @param ids array of target plugin IDs
         * @return this builder instance
         */
        public Builder loadBefore(String... ids) {
            if (ids != null) {
                this.loadBefore.addAll(Arrays.asList(ids));
            }
            return this;
        }

        /**
         * Appends a single plugin ID that must load after this plugin.
         *
         * @param id target plugin ID
         * @return this builder instance
         */
        public Builder addLoadBefore(String id) {
            if (id != null && !id.isBlank()) {
                this.loadBefore.add(id);
            }
            return this;
        }

        /**
         * Appends a list of plugin IDs that must load before this plugin.
         *
         * @param ids list of prerequisite plugin IDs
         * @return this builder instance
         */
        public Builder loadAfter(List<String> ids) {
            if (ids != null) {
                this.loadAfter.addAll(ids);
            }
            return this;
        }

        /**
         * Appends multiple plugin IDs that must load before this plugin.
         *
         * @param ids array of prerequisite plugin IDs
         * @return this builder instance
         */
        public Builder loadAfter(String... ids) {
            if (ids != null) {
                this.loadAfter.addAll(Arrays.asList(ids));
            }
            return this;
        }

        /**
         * Appends a single plugin ID that must load before this plugin.
         *
         * @param id prerequisite plugin ID
         * @return this builder instance
         */
        public Builder addLoadAfter(String id) {
            if (id != null && !id.isBlank()) {
                this.loadAfter.add(id);
            }
            return this;
        }

        /**
         * Sets the fully-qualified name of the main plugin entrypoint class.
         *
         * @param entrypoint binary name of the entrypoint class
         * @return this builder instance
         */
        public Builder entrypoint(String entrypoint) {
            this.entrypoint = entrypoint;
            return this;
        }

        /**
         * Appends a list of mixin configuration paths.
         *
         * @param mixins list of mixin configuration JSON/YAML paths
         * @return this builder instance
         */
        public Builder mixins(List<String> mixins) {
            if (mixins != null) {
                this.mixins.addAll(mixins);
            }
            return this;
        }

        /**
         * Appends multiple mixin configuration paths.
         *
         * @param mixins array of mixin configuration paths
         * @return this builder instance
         */
        public Builder mixins(String... mixins) {
            if (mixins != null) {
                this.mixins.addAll(Arrays.asList(mixins));
            }
            return this;
        }

        /**
         * Appends a single mixin configuration path.
         *
         * @param mixin relative path to mixin config inside JAR
         * @return this builder instance
         */
        public Builder addMixin(String mixin) {
            if (mixin != null && !mixin.isBlank()) {
                this.mixins.add(mixin);
            }
            return this;
        }

        /**
         * Builds and validates the resulting {@link Metadata} record.
         *
         * @return validated immutable {@link Metadata} instance
         * @throws NullPointerException     if any mandatory string field ({@code id}, {@code name}, {@code version}) is {@code null}
         * @throws IllegalArgumentException if domain constraints or mandatory fields are violated
         */
        public Metadata build() {
            return new Metadata(
                    schema,
                    name,
                    description,
                    id,
                    version,
                    environment,
                    authors,
                    license,
                    contacts,
                    dependencies,
                    loadBefore,
                    loadAfter,
                    entrypoint,
                    mixins
            );
        }
    }
}