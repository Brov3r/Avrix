package com.avrix.plugins;

import com.avrix.core.Environment;
import com.avrix.core.Metadata;
import com.avrix.utils.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Objects;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Parses plugin manifests from JAR archives and YAML files.
 */
public final class PluginManifestParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(PluginManifestParser.class);

    /**
     * Constructs a new plugin manifest parser.
     */
    public PluginManifestParser() {
    }

    /**
     * Parses a plugin metadata manifest located inside a JAR file.
     *
     * @param jarPath   the path to the physical JAR file on disk
     * @param entryPath internal relative path to the YAML manifest (e.g., {@code "metadata.yml"})
     * @return parsed immutable metadata
     * @throws NullPointerException  if {@code jarPath} or {@code entryPath} is {@code null}
     * @throws FileNotFoundException if the specified entry is missing inside the JAR
     * @throws IOException           if an I/O error occurs while reading the JAR or parsing YAML
     */
    public Metadata fromJarFile(Path jarPath, String entryPath) throws IOException {
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
    public Metadata fromJarFile(File jarFile, String entryPath) throws IOException {
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
    public Metadata fromYaml(Path path) throws IOException {
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
    public Metadata fromNode(ConfigurationNode node) throws SerializationException {
        Objects.requireNonNull(node, "ConfigurationNode cannot be null");

        Metadata.Builder builder = new Metadata.Builder()
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
}
