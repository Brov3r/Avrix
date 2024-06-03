package com.avrix.plugin;

import com.avrix.enums.PluginEnvironment;
import com.avrix.utils.Constants;
import com.avrix.utils.YamlFile;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Represents metadata for a plugin, including various details such as name, description, author, version, and dependencies.
 */
public final class Metadata {
    private String name; // Name of the module (plugin)
    private String description; // Description of the module (plugin)
    private String id; // Module (plugin) identifier
    private String author; // Author of the code
    private String version; // Current version of the module (plugin)
    private String license; // License under which the code is distributed
    private String contacts; // Author's contact information
    private PluginEnvironment environment; // Environment in which the plugin should run (client, server)
    private List<String> entryPointsList; // List of entry points as full class path
    private List<String> patchList; // List of classes that modify game code as a full class path
    private Map<String, String> dependenciesMap; // Dependency map, where the key is the identifier of the module (plugin), and the value is its version

    /**
     * Private constructor to prevent direct instantiation.
     * Use {@link MetadataBuilder} to create instances.
     */
    private Metadata() {
        this.name = null;
        this.id = null;
        this.description = null;
        this.author = null;
        this.version = null;
        this.license = null;
        this.environment = null;
        this.contacts = null;
        this.entryPointsList = null;
        this.dependenciesMap = null;
        this.patchList = null;
    }

    /**
     * Creating metadata from a file inside a Jar archive
     *
     * @param jarFile          Jar archive in the form {@link File}
     * @param metadataFileName full path to the YAML file with metadata in the form {@link String}
     * @return if creation is successful - {@link Metadata} object, if an error occurs - null
     */
    public static Metadata createFromJar(File jarFile, String metadataFileName) {
        try {
            YamlFile yamlFile = new YamlFile(jarFile.getAbsolutePath(), metadataFileName);

            return new Metadata.MetadataBuilder()
                    .name(yamlFile.getString("name"))
                    .id(yamlFile.getString("id"))
                    .description(yamlFile.getString("description"))
                    .author(yamlFile.getString("author"))
                    .version(yamlFile.getString("version"))
                    .license(yamlFile.getString("license"))
                    .contacts(yamlFile.getString("contacts"))
                    .environment(yamlFile.getString("environment"))
                    .entryPointsList(yamlFile.getStringList("entrypoints"))
                    .patchList(yamlFile.getStringList("patches"))
                    .dependencies(yamlFile.getStringMap("dependencies"))
                    .build();
        } catch (Exception e) {
            System.out.printf("[!] Failed to generate metadata '%s' due to: %s%n", jarFile.getName(), e.getMessage().replace("[!] ", ""));
            return null;
        }
    }

    /**
     * Returns a {@link File} object representing the configuration directory for this plugin.
     * The directory path is normalized to prevent problems with various file systems.
     *
     * @return A {@link File} object pointing to the normalized path to the plugin configuration directory.
     */
    public File getConfigFolder() {
        return new File(Constants.PLUGINS_FOLDER_NAME, getId());
    }

    /**
     * Returns the name of the plugin.
     *
     * @return the name of the plugin
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the {@link PluginEnvironment} of the plugin.
     *
     * @return the {@link PluginEnvironment} of the plugin
     */
    public PluginEnvironment getEnvironment() {
        return environment;
    }

    /**
     * Returns the ID of the plugin.
     *
     * @return the ID of the plugin
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the description of the plugin.
     *
     * @return the description of the plugin
     */
    public String getDescription() {
        return description;
    }

    /**
     * Returns the author of the plugin.
     *
     * @return the author of the plugin
     */
    public String getAuthor() {
        return author;
    }

    /**
     * Returns the version of the plugin.
     *
     * @return the version of the plugin
     */
    public String getVersion() {
        return version;
    }

    /**
     * Returns the license of the plugin.
     *
     * @return the license of the plugin
     */
    public String getLicense() {
        return license;
    }

    /**
     * Returns the contact information for the plugin author.
     *
     * @return the contact information for the plugin author
     */
    public String getContacts() {
        return contacts;
    }

    /**
     * Returns the {@link List} of entry points for the plugin.
     *
     * @return the {@link List} of entry points for the plugin
     */
    public List<String> getEntryPoints() {
        return entryPointsList;
    }

    /**
     * Returns the {@link List} of patches for the plugin.
     *
     * @return the {@link List} of patches for the plugin
     */
    public List<String> getPatchList() {
        return patchList;
    }

    /**
     * Returns the {@link Map} of dependencies for the plugin.
     *
     * @return the {@link Map} of dependencies for the plugin
     */
    public Map<String, String> getDependencies() {
        return dependenciesMap;
    }

    /**
     * Builder class for constructing {@link Metadata} instances.
     */
    public final static class MetadataBuilder {
        private final Metadata metadata;

        /**
         * Creates a new builder instance.
         */
        public MetadataBuilder() {
            metadata = new Metadata();
        }

        /**
         * Sets the name of the plugin.
         *
         * @param name the name of the plugin
         * @return the builder instance
         */
        public MetadataBuilder name(String name) {
            metadata.name = name;
            return this;
        }

        /**
         * Sets the ID of the plugin.
         *
         * @param id the ID of the plugin
         * @return the builder instance
         */
        public MetadataBuilder id(String id) {
            metadata.id = id;
            return this;
        }

        /**
         * Sets the description of the plugin.
         *
         * @param description the description of the plugin
         * @return the builder instance
         */
        public MetadataBuilder description(String description) {
            metadata.description = description;
            return this;
        }

        /**
         * Sets the author of the plugin.
         *
         * @param author the author of the plugin
         * @return the builder instance
         */
        public MetadataBuilder author(String author) {
            metadata.author = author;
            return this;
        }

        /**
         * Sets the environment of the plugin.
         *
         * @param environment the environment of the plugin
         * @return the builder instance
         */
        public MetadataBuilder environment(String environment) {
            metadata.environment = PluginEnvironment.fromString(environment);
            return this;
        }

        /**
         * Sets the version of the plugin.
         *
         * @param version the version of the plugin
         * @return the builder instance
         */
        public MetadataBuilder version(String version) {
            metadata.version = version;
            return this;
        }

        /**
         * Sets the license of the plugin.
         *
         * @param license the license of the plugin
         * @return the builder instance
         */
        public MetadataBuilder license(String license) {
            metadata.license = license;
            return this;
        }

        /**
         * Sets the contact information for the plugin author.
         *
         * @param contacts the contact information for the plugin author
         * @return the builder instance
         */
        public MetadataBuilder contacts(String contacts) {
            metadata.contacts = contacts;
            return this;
        }

        /**
         * Sets the {@link List} of entry points for the plugin.
         *
         * @param entryPointsList the list of entry points for the plugin
         * @return the builder instance
         */
        public MetadataBuilder entryPointsList(List<String> entryPointsList) {
            metadata.entryPointsList = entryPointsList;
            return this;
        }

        /**
         * Sets the {@link List} of patches for the plugin.
         *
         * @param patchList the list of patches for the plugin
         * @return the builder instance
         */
        public MetadataBuilder patchList(List<String> patchList) {
            metadata.patchList = patchList;
            return this;
        }

        /**
         * Sets the {@link Map} of dependencies for the plugin.
         *
         * @param dependencies the map of dependencies for the plugin
         * @return the builder instance
         */
        public MetadataBuilder dependencies(Map<String, String> dependencies) {
            metadata.dependenciesMap = dependencies;
            return this;
        }

        /**
         * Builds and returns the {@link Metadata} instance.
         * Ensures that all required fields are set.
         *
         * @return the constructed {@link Metadata} instance
         * @throws NullPointerException if any required field is not set
         */
        public Metadata build() {
            validate();
            return metadata;
        }

        /**
         * Validates that all required fields are set.
         *
         * @throws NullPointerException if any required field is not set
         */
        private void validate() {
            Objects.requireNonNull(metadata.name, "[!] The required field 'name' is not specified in the metadata!");
            Objects.requireNonNull(metadata.id, "[!] The required field 'id' is not specified in the metadata!");
            Objects.requireNonNull(metadata.license, "[!] The required field 'license' is not specified in the metadata!");
            Objects.requireNonNull(metadata.author, "[!] The required field 'author' is not specified in the metadata!");
            Objects.requireNonNull(metadata.version, "[!] The required field 'version' is not specified in the metadata!");
            Objects.requireNonNull(metadata.environment, "[!] The required field 'environment' is not specified in the metadata!");
            Objects.requireNonNull(metadata.entryPointsList, "[!] The required field 'entry points' is not specified in the metadata!");
        }
    }
}