package com.avrix.core;

import com.avrix.utils.Constants;

import java.util.*;

/**
 * Immutable metadata descriptor for an Avrix plugin or runtime game provider.
 * <p>
 * Encapsulates all declarative manifest information parsed from {@code metadata.yml} or synthesized by an
 * active game provider. Used for topological dependency sorting, environment filtering, and mixin bootstrap
 * configuration.
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
 * @param entrypoint   entrypoint class name; for user plugins this is a {@link com.avrix.plugins.Plugin}
 *                     implementation, for game provider descriptors it is the game's main class, and for loader
 *                     descriptors it is empty
 * @param mixins       unmodifiable list of fully qualified mixin class names registered with the loader
 * @param conflicts    unmodifiable list of plugin IDs declared incompatible with this plugin
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
        List<String> mixins,
        List<String> conflicts
) {

    /**
     * Compact constructor enforcing domain invariants, normalizing string values, and collection immutability.
     *
     * @throws NullPointerException     if any mandatory string field ({@code id}, {@code name}, {@code version}) is {@code null}
     * @throws IllegalArgumentException if string invariants (non-blank, no embedded whitespace in identifiers) or supported
     *                                  schema versions are violated
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

        id = id.strip();
        name = name.strip();
        version = version.strip();

        if (id.isBlank()) {
            throw new IllegalArgumentException("Plugin 'id' cannot be blank");
        }
        if (name.isBlank()) {
            throw new IllegalArgumentException("Plugin 'name' cannot be blank");
        }
        if (version.isBlank()) {
            throw new IllegalArgumentException("Plugin 'version' cannot be blank");
        }
        if (id.chars().anyMatch(Character::isWhitespace)) {
            throw new IllegalArgumentException("Plugin 'id' cannot contain whitespace");
        }
        if (id.chars().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("Plugin 'id' cannot contain control characters");
        }

        // Optional scalar fields normalization
        environment = Objects.requireNonNullElse(environment, Environment.BOTH);
        description = Objects.requireNonNullElse(description, "").strip();
        license = Objects.requireNonNullElse(license, "UNLICENSED").strip();
        entrypoint = Objects.requireNonNullElse(entrypoint, "").strip();
        if (license.isBlank()) {
            license = "UNLICENSED";
        }

        // Defensive copy, normalization, and unmodifiable collection creation
        authors = normalizeList(authors);
        contacts = normalizeList(contacts);
        mixins = normalizeList(mixins);
        dependencies = normalizeDependencies(dependencies);
        loadBefore = normalizeList(loadBefore);
        loadAfter = normalizeList(loadAfter);
        conflicts = normalizeList(conflicts);
    }

    /**
     * Creates an immutable list with trimmed, non-blank string values.
     *
     * @param values source values, may be {@code null}
     * @return immutable normalized list
     */
    private static List<String> normalizeList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }

        List<String> normalized = new ArrayList<>(values.size());
        for (String value : values) {
            if (value == null) {
                continue;
            }
            String trimmed = value.strip();
            if (!trimmed.isEmpty()) {
                normalized.add(trimmed);
            }
        }
        return List.copyOf(normalized);
    }

    /**
     * Creates an immutable dependency map with trimmed identifiers and constraints.
     *
     * @param values source dependency map, may be {@code null}
     * @return immutable normalized dependency map
     * @throws NullPointerException if a dependency identifier or constraint is {@code null}
     */
    private static Map<String, String> normalizeDependencies(Map<String, String> values) {
        if (values == null || values.isEmpty()) {
            return Map.of();
        }

        Map<String, String> normalized = new LinkedHashMap<>(values.size());
        for (Map.Entry<String, String> entry : values.entrySet()) {
            normalized.put(entry.getKey().strip(), entry.getValue().strip());
        }
        return Map.copyOf(normalized);
    }

    /**
     * Fluent Builder for constructing immutable {@link Metadata} instances.
     */
    public static final class Builder {

        /**
         * Constructs an empty metadata builder with default values.
         */
        public Builder() {
        }

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
        private final List<String> conflicts = new ArrayList<>();

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
         * Appends a list of fully qualified mixin class names.
         *
         * @param mixins list of mixin class names
         * @return this builder instance
         */
        public Builder mixins(List<String> mixins) {
            if (mixins != null) {
                this.mixins.addAll(mixins);
            }
            return this;
        }

        /**
         * Appends multiple fully qualified mixin class names.
         *
         * @param mixins array of mixin class names
         * @return this builder instance
         */
        public Builder mixins(String... mixins) {
            if (mixins != null) {
                this.mixins.addAll(Arrays.asList(mixins));
            }
            return this;
        }

        /**
         * Appends a single fully qualified mixin class name.
         *
         * @param mixin mixin class name
         * @return this builder instance
         */
        public Builder addMixin(String mixin) {
            if (mixin != null && !mixin.isBlank()) {
                this.mixins.add(mixin);
            }
            return this;
        }

        /**
         * Appends a list of plugin identifiers that are incompatible with this plugin.
         *
         * @param conflicts list of conflicting plugin identifiers
         * @return this builder instance
         */
        public Builder conflicts(List<String> conflicts) {
            if (conflicts != null) {
                this.conflicts.addAll(conflicts);
            }
            return this;
        }

        /**
         * Appends multiple plugin identifiers that are incompatible with this plugin.
         *
         * @param conflicts array of conflicting plugin identifiers
         * @return this builder instance
         */
        public Builder conflicts(String... conflicts) {
            if (conflicts != null) {
                this.conflicts.addAll(Arrays.asList(conflicts));
            }
            return this;
        }

        /**
         * Appends a single plugin identifier that is incompatible with this plugin.
         *
         * @param conflict conflicting plugin identifier
         * @return this builder instance
         */
        public Builder addConflict(String conflict) {
            if (conflict != null && !conflict.isBlank()) {
                this.conflicts.add(conflict);
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
                    mixins,
                    conflicts
            );
        }
    }
}