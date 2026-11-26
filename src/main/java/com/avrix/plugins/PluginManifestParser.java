package com.avrix.plugins;

import com.avrix.core.Environment;
import com.avrix.core.Metadata;
import com.avrix.utils.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.loader.ParsingException;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Parses plugin manifests from JAR archives and YAML files.
 */
public final class PluginManifestParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(PluginManifestParser.class);
    private final boolean strict;

    private static final Set<String> KNOWN_KEYS = Set.of(
            "schema",
            "id",
            "name",
            "description",
            "version",
            "environment",
            "license",
            "authors",
            "contacts",
            "dependencies",
            "loadBefore",
            "loadAfter",
            "entrypoint",
            "entrypoints",
            "mixins",
            "conflicts"
    );

    private static final List<String> ENTRYPOINT_SIDES = List.of("main", "client", "server");

    private static final List<String> MIXIN_SIDES = List.of("common", "client", "server");

    /**
     * Constructs a lenient plugin manifest parser.
     */
    public PluginManifestParser() {
        this(false);
    }

    /**
     * Constructs a parser with an explicit environment validation policy.
     *
     * @param strict whether unknown environment values must be rejected instead of defaulting to both
     */
    public PluginManifestParser(boolean strict) {
        this.strict = strict;
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

        String source = "Invalid metadata in JAR [%s] at entry [%s]".formatted(jarPath, entryPath);
        LOGGER.debug("Loading plugin metadata from JAR [{}] at entry [{}]", jarPath, entryPath);
        try (JarFile jar = openJar(jarPath)) {
            return parseJarEntry(jar, jarPath, entryPath, source);
        }
    }

    /**
     * Parses a plugin manifest and collects class entries from the same open archive.
     * <p>
     * This avoids reopening the plugin JAR later when building the class resource index.
     *
     * @param jarPath   the path to the physical JAR file on disk
     * @param entryPath internal relative path to the YAML manifest
     * @return parsed metadata together with class entries of the same JAR
     * @throws NullPointerException  if {@code jarPath} or {@code entryPath} is {@code null}
     * @throws FileNotFoundException if the specified entry is missing inside the JAR
     * @throws IOException           if an I/O error occurs while reading the JAR or parsing YAML
     */
    public ParsedJarManifest fromJarFileWithClassEntries(Path jarPath, String entryPath) throws IOException {
        return fromJarFileWithClassEntries(jarPath, entryPath, null);
    }

    /**
     * Parses a plugin manifest and collects archive metadata from the same open archive.
     * <p>
     * The returned descriptor also reports whether the archive is multi-release and whether it contains the
     * requested icon. This avoids reopening the archive later during plugin preparation.
     *
     * @param jarPath   the path to the physical JAR file on disk
     * @param entryPath internal relative path to the YAML manifest
     * @param iconName  icon entry name to probe, or {@code null} to skip the icon check
     * @return parsed metadata together with archive class entries and archive flags
     * @throws NullPointerException  if {@code jarPath} or {@code entryPath} is {@code null}
     * @throws FileNotFoundException if the specified entry is missing inside the JAR
     * @throws IOException           if an I/O error occurs while reading the JAR or parsing YAML
     */
    public ParsedJarManifest fromJarFileWithClassEntries(
            Path jarPath,
            String entryPath,
            String iconName
    ) throws IOException {
        Objects.requireNonNull(jarPath, "JAR path cannot be null");
        Objects.requireNonNull(entryPath, "Entry path cannot be null");

        String source = "Invalid metadata in JAR [%s] at entry [%s]".formatted(jarPath, entryPath);
        LOGGER.debug("Loading plugin metadata from JAR [{}] at entry [{}]", jarPath, entryPath);
        try (JarFile jar = openJar(jarPath)) {
            Metadata metadata = parseJarEntry(jar, jarPath, entryPath, source);
            boolean iconPresent = iconName != null && !iconName.isBlank() && jar.getJarEntry(iconName) != null;
            return new ParsedJarManifest(
                    metadata,
                    collectClassEntries(jar),
                    jar.isMultiRelease(),
                    iconPresent
            );
        }
    }

    /**
     * Opens a plugin JAR for metadata parsing.
     *
     * @param jarPath physical JAR path
     * @return open JAR file
     * @throws IOException if the archive cannot be opened
     */
    private static JarFile openJar(Path jarPath) throws IOException {
        try {
            return new JarFile(jarPath.toFile(), false);
        } catch (IOException e) {
            LOGGER.error(
                    "Failed to parse metadata manifest from JAR [{}]: {}",
                    jarPath.getFileName(),
                    e.getMessage(),
                    e
            );
            throw e;
        }
    }

    /**
     * Parses a manifest entry from an already-open JAR.
     *
     * @param jar       open plugin archive
     * @param jarPath   physical JAR path used for diagnostics
     * @param entryPath internal manifest entry path
     * @param source    diagnostic source description
     * @return parsed immutable metadata
     * @throws IOException if reading or parsing fails
     */
    private Metadata parseJarEntry(JarFile jar, Path jarPath, String entryPath, String source) throws IOException {
        try {
            JarEntry entry = jar.getJarEntry(entryPath);
            if (entry == null) {
                throw new FileNotFoundException(
                        "Metadata entry [%s] not found in JAR [%s]".formatted(entryPath, jarPath)
                );
            }

            try (InputStream in = jar.getInputStream(entry)) {
                YamlConfigurationLoader loader = YamlConfigurationLoader.builder()
                        .source(() -> new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)))
                        .build();
                enableDuplicateKeyDetection(loader);
                Metadata metadata = parseNode(loader.load(), source);
                LOGGER.debug("Successfully loaded metadata for plugin [{}] (v{})", metadata.id(), metadata.version());
                return metadata;
            }
        } catch (FileNotFoundException e) {
            LOGGER.warn("Metadata manifest not found: {}", e.getMessage());
            throw e;
        } catch (SerializationException e) {
            throw e;
        } catch (ParsingException e) {
            throw withCause(source + ": " + parseFailureMessage(e), e);
        } catch (IOException e) {
            LOGGER.error(
                    "Failed to parse metadata manifest from JAR [{}]: {}",
                    jarPath.getFileName(),
                    e.getMessage(),
                    e
            );
            throw e;
        } catch (RuntimeException e) {
            throw withCause(source + ": " + parseFailureMessage(e), e);
        }
    }

    /**
     * Collects class entries from an open plugin archive.
     * <p>
     * Package-private so the plugin manager can reuse the scan for classpath conflict reporting when entries were not
     * collected during manifest parsing.
     *
     * @param jar open plugin archive
     * @return immutable list of class resource paths
     */
    static List<String> collectClassEntries(JarFile jar) {
        List<String> classEntries = new ArrayList<>();
        Enumeration<JarEntry> entries = jar.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            if (entry.isDirectory()) {
                continue;
            }

            String entryName = entry.getName();
            if (entryName.endsWith(".class")
                    && !entryName.startsWith("META-INF/")
                    && !"module-info.class".equals(entryName)) {
                classEntries.add(entryName);
            }
        }
        return List.copyOf(classEntries);
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
        String source = "Invalid metadata in YAML [%s]".formatted(path);
        YamlConfigurationLoader loader = YamlConfigurationLoader.builder().path(path).build();
        enableDuplicateKeyDetection(loader);
        try {
            return parseNode(loader.load(), source);
        } catch (SerializationException e) {
            throw e;
        } catch (ParsingException e) {
            throw withCause(source + ": " + parseFailureMessage(e), e);
        } catch (RuntimeException e) {
            throw withCause(source + ": " + parseFailureMessage(e), e);
        }
    }

    /**
     * Deserializes a {@link Metadata} record from a Configurate {@link ConfigurationNode}.
     * <p>
     * Duplicate YAML key detection is available only through the file- and JAR-based parsing methods.
     *
     * @param node the root configuration node
     * @return populated {@link Metadata}
     * @throws NullPointerException   if {@code node} is {@code null}
     * @throws SerializationException if required nodes cannot be deserialized
     */
    public Metadata fromNode(ConfigurationNode node) throws SerializationException {
        return parseNode(node, "Invalid metadata");
    }

    /**
     * Parses a manifest node and dispatches to the parser for the declared schema version.
     *
     * @param node   root configuration node
     * @param source diagnostic source description
     * @return parsed immutable metadata
     * @throws SerializationException if validation fails or the schema version is unsupported
     */
    private Metadata parseNode(ConfigurationNode node, String source) throws SerializationException {
        Objects.requireNonNull(node, "ConfigurationNode cannot be null");
        validateTopLevelKeys(node, source);

        int schema = readSchema(node, source);
        return switch (schema) {
            case Constants.METADATA_SCHEMA -> parseSchema1(node, source);
            case Constants.METADATA_SCHEMA_2 -> parseSchema2(node, source);
            default -> throw new SerializationException(
                    "%s: Unsupported metadata schema [%d], supported: %d, %d"
                            .formatted(
                                    source,
                                    schema,
                                    Constants.METADATA_SCHEMA,
                                    Constants.METADATA_SCHEMA_2
                            )
            );
        };
    }

    /**
     * Parses a manifest that declares the legacy schema version.
     *
     * @param node   root configuration node
     * @param source diagnostic source description
     * @return parsed immutable metadata
     * @throws SerializationException if any manifest value is invalid or schema-2 syntax is used
     */
    private Metadata parseSchema1(ConfigurationNode node, String source) throws SerializationException {
        if (isPresent(node.node("entrypoints"))) {
            throw new SerializationException(
                    source + ": Metadata key [entrypoints] requires metadata schema " + Constants.METADATA_SCHEMA_2
            );
        }

        ConfigurationNode mixinsNode = node.node("mixins");
        if (isPresent(mixinsNode) && mixinsNode.isMap()) {
            throw new SerializationException(
                    source + ": Metadata key [mixins] must be a list of strings in metadata schema "
                            + Constants.METADATA_SCHEMA
            );
        }

        String id = node.node("id").getString();
        String entrypoint = node.node("entrypoint").getString("");
        List<String> mixins = readStringList(node, "mixins", source);

        Metadata.Builder builder = new Metadata.Builder()
                .schema(Constants.METADATA_SCHEMA)
                .id(id)
                .name(node.node("name").getString())
                .description(node.node("description").getString(""))
                .version(node.node("version").getString())
                .entrypoint(entrypoint)
                .license(node.node("license").getString("UNLICENSED"))
                .environment(readEnvironment(node, source));

        builder.loadBefore(readIdentifierList(node, "loadBefore", source, true));
        builder.loadAfter(readIdentifierList(node, "loadAfter", source, true));
        builder.authors(readStringList(node, "authors", source));
        builder.contacts(readStringList(node, "contacts", source));
        builder.mixins(mixins);
        builder.conflicts(readIdentifierList(node, "conflicts", source, false));

        for (Map.Entry<String, String> dependency : readDependencies(node, source).entrySet()) {
            builder.addDependency(dependency.getKey(), dependency.getValue());
        }

        Metadata metadata;
        try {
            metadata = builder.build();
        } catch (RuntimeException e) {
            throw withCause(source + ": Invalid metadata: " + e.getMessage(), e);
        }

        return metadata;
    }

    /**
     * Parses a schema-2 manifest with side-specific entrypoints and mixins.
     * <p>
     * The singular {@code entrypoint} key is accepted as a shorthand for the main side, and a flat {@code mixins} list
     * is accepted as a shorthand for the common side. The two entrypoint forms cannot be combined.
     *
     * @param node   root configuration node
     * @param source diagnostic source description
     * @return parsed immutable metadata
     * @throws SerializationException if any manifest value is invalid, side sections are unknown, or both entrypoint
     *                                forms are declared
     */
    private Metadata parseSchema2(ConfigurationNode node, String source) throws SerializationException {
        ClassSections entrypoints = readEntrypointSections(node, source);
        ClassSections mixins = readMixinSections(node, source);

        Metadata.Builder builder = new Metadata.Builder()
                .schema(Constants.METADATA_SCHEMA_2)
                .id(node.node("id").getString())
                .name(node.node("name").getString())
                .description(node.node("description").getString(""))
                .version(node.node("version").getString())
                .license(node.node("license").getString("UNLICENSED"))
                .environment(readEnvironment(node, source))
                .mainEntrypoints(entrypoints.shared())
                .clientEntrypoints(entrypoints.client())
                .serverEntrypoints(entrypoints.server())
                .mixins(mixins.shared())
                .clientMixins(mixins.client())
                .serverMixins(mixins.server());

        builder.loadBefore(readIdentifierList(node, "loadBefore", source, true));
        builder.loadAfter(readIdentifierList(node, "loadAfter", source, true));
        builder.authors(readStringList(node, "authors", source));
        builder.contacts(readStringList(node, "contacts", source));
        builder.conflicts(readIdentifierList(node, "conflicts", source, false));

        for (Map.Entry<String, String> dependency : readDependencies(node, source).entrySet()) {
            builder.addDependency(dependency.getKey(), dependency.getValue());
        }

        Metadata metadata;
        try {
            metadata = builder.build();
        } catch (RuntimeException e) {
            throw withCause(source + ": Invalid metadata: " + e.getMessage(), e);
        }

        return metadata;
    }

    /**
     * Reads all entrypoint sections from a schema-2 manifest.
     *
     * @param node   manifest root node
     * @param source diagnostic source description
     * @return immutable entrypoint sections; the shared section contains main entrypoints
     * @throws SerializationException if both entrypoint forms are declared or a section is malformed
     */
    private ClassSections readEntrypointSections(ConfigurationNode node, String source)
            throws SerializationException {
        ConfigurationNode singularNode = node.node("entrypoint");
        ConfigurationNode sectionsNode = node.node("entrypoints");
        boolean singularPresent = isPresent(singularNode);
        boolean sectionsPresent = isPresent(sectionsNode);

        if (singularPresent && sectionsPresent) {
            throw new SerializationException(
                    source + ": Metadata keys [entrypoint] and [entrypoints] cannot be combined"
            );
        }
        if (singularPresent) {
            return new ClassSections(readSingleClass(singularNode, "entrypoint", source), List.of(), List.of());
        }
        if (!sectionsPresent) {
            return new ClassSections(List.of(), List.of(), List.of());
        }

        requireSectionMap(sectionsNode, "entrypoints", source);
        validateSectionKeys(sectionsNode, "entrypoints", ENTRYPOINT_SIDES, source);
        return new ClassSections(
                readClassList(sectionsNode.node("main"), "entrypoints.main", source),
                readClassList(sectionsNode.node("client"), "entrypoints.client", source),
                readClassList(sectionsNode.node("server"), "entrypoints.server", source)
        );
    }

    /**
     * Reads all mixin sections from a schema-2 manifest.
     *
     * @param node   manifest root node
     * @param source diagnostic source description
     * @return immutable mixin sections; the shared section contains common mixins
     * @throws SerializationException if a section is malformed or an unknown side is declared
     */
    private ClassSections readMixinSections(ConfigurationNode node, String source) throws SerializationException {
        ConfigurationNode mixinsNode = node.node("mixins");
        if (!isPresent(mixinsNode)) {
            return new ClassSections(List.of(), List.of(), List.of());
        }

        if (!mixinsNode.isMap()) {
            return new ClassSections(readClassList(mixinsNode, "mixins", source), List.of(), List.of());
        }

        validateSectionKeys(mixinsNode, "mixins", MIXIN_SIDES, source);
        return new ClassSections(
                readClassList(mixinsNode.node("common"), "mixins.common", source),
                readClassList(mixinsNode.node("client"), "mixins.client", source),
                readClassList(mixinsNode.node("server"), "mixins.server", source)
        );
    }

    /**
     * Reads a single entrypoint class name from the legacy shorthand key.
     *
     * @param entrypointNode scalar entrypoint node
     * @param key            manifest key used in diagnostics
     * @param source         diagnostic source description
     * @return immutable singleton list, or an empty list when the value is blank
     * @throws SerializationException if the value is not a scalar string
     */
    private List<String> readSingleClass(ConfigurationNode entrypointNode, String key, String source)
            throws SerializationException {
        if (entrypointNode.isMap() || entrypointNode.isList()) {
            throw new SerializationException(source + ": Metadata key [" + key + "] must be a string");
        }

        String value = entrypointNode.getString("");
        String normalized = value == null ? "" : value.strip();
        if (normalized.isEmpty()) {
            return List.of();
        }
        return List.of(normalized);
    }

    /**
     * Reads a side section and verifies that it is represented as a map.
     *
     * @param sectionNode section node
     * @param section     section name used in diagnostics
     * @param source      diagnostic source description
     * @throws SerializationException if the section is not a map
     */
    private void requireSectionMap(ConfigurationNode sectionNode, String section, String source)
            throws SerializationException {
        if (!sectionNode.isMap()) {
            throw new SerializationException(
                    source + ": Metadata key [" + section + "] must be a map of side names to string lists"
            );
        }
    }

    /**
     * Rejects unknown side names inside a schema-2 section.
     *
     * @param sectionNode section node
     * @param section     section name used in diagnostics
     * @param allowed     allowed side names in declaration order
     * @param source      diagnostic source description
     * @throws SerializationException if an unknown side name is present
     */
    private void validateSectionKeys(
            ConfigurationNode sectionNode,
            String section,
            List<String> allowed,
            String source
    ) throws SerializationException {
        for (Object key : sectionNode.childrenMap().keySet()) {
            String side = String.valueOf(key);
            if (!allowed.contains(side)) {
                throw new SerializationException(
                        source + ": Unknown key [" + side + "] in metadata section [" + section
                                + "], allowed: " + String.join(", ", allowed)
                );
            }
        }
    }

    /**
     * Reads a list of class names, skipping blanks and removing duplicates by first occurrence.
     *
     * @param listNode list node, may be virtual
     * @param label    manifest key used in diagnostics
     * @param source   diagnostic source description
     * @return immutable normalized class-name list
     * @throws SerializationException if the node is a map or cannot be read as a string list
     */
    private List<String> readClassList(ConfigurationNode listNode, String label, String source)
            throws SerializationException {
        if (isPresent(listNode) && !listNode.isList()) {
            throw new SerializationException(
                    "%s: Metadata key [%s] must be a list of strings".formatted(source, label)
            );
        }
        List<String> values = readStringListNode(listNode, label, source);
        return deduplicate(values);
    }

    /**
     * Removes duplicate strings while preserving the first occurrence order.
     *
     * @param values source values
     * @return immutable list without duplicates
     */
    private static List<String> deduplicate(List<String> values) {
        List<String> unique = new ArrayList<>(values.size());
        Set<String> seen = new HashSet<>();
        for (String value : values) {
            if (seen.add(value)) {
                unique.add(value);
            }
        }
        return List.copyOf(unique);
    }

    /**
     * Checks whether a configuration node carries a declared value.
     *
     * @param node configuration node
     * @return {@code true} if the node is neither virtual nor null
     */
    private static boolean isPresent(ConfigurationNode node) {
        return !node.virtual() && !node.isNull();
    }

    /**
     * Reads a string list, trimming values and rejecting blank entries.
     *
     * @param node   manifest root node
     * @param key    manifest key containing the list
     * @param source diagnostic source description
     * @return immutable normalized list
     * @throws SerializationException if the value is not a string list
     */
    private List<String> readStringList(
            ConfigurationNode node,
            String key,
            String source
    ) throws SerializationException {
        return readStringListNode(node.node(key), key, source);
    }

    /**
     * Reads a string list from a specific node, trimming values and skipping blank entries.
     *
     * @param listNode list node, may be virtual
     * @param label    manifest key used in diagnostics
     * @param source   diagnostic source description
     * @return immutable normalized list
     * @throws SerializationException if the node is a map or cannot be read as a string list
     */
    private List<String> readStringListNode(
            ConfigurationNode listNode,
            String label,
            String source
    ) throws SerializationException {
        if (listNode.virtual() || listNode.isNull()) {
            return Collections.emptyList();
        }
        if (listNode.isMap()) {
            throw new SerializationException(
                    "%s: Metadata key [%s] must be a list of strings".formatted(source, label)
            );
        }

        List<String> values;
        try {
            values = listNode.getList(String.class, Collections.emptyList());
        } catch (SerializationException e) {
            throw withCause("%s: Metadata key [%s] must be a list of strings".formatted(source, label), e);
        }

        List<String> normalized = new ArrayList<>(values.size());
        for (String value : values) {
            if (value == null || value.isBlank()) {
                warn(source, "Metadata key [%s] contains a blank list entry".formatted(label));
                continue;
            }
            normalized.add(value.strip());
        }
        return List.copyOf(normalized);
    }

    /**
     * Reads a list of plugin identifiers, validating every entry.
     *
     * @param node          manifest root node
     * @param key           manifest key containing the list
     * @param source        diagnostic source description
     * @param allowWildcard whether the special {@code "*"} value is allowed
     * @return immutable normalized list without duplicates
     * @throws SerializationException if the list is malformed
     */
    private List<String> readIdentifierList(
            ConfigurationNode node,
            String key,
            String source,
            boolean allowWildcard
    ) throws SerializationException {
        List<String> values = readStringList(node, key, source);
        List<String> normalized = new ArrayList<>(values.size());
        Set<String> seen = new HashSet<>();

        for (String value : values) {
            if (allowWildcard && "*".equals(value)) {
                if (!seen.add(value)) {
                    warn(source, "Metadata key [%s] contains duplicate identifier [%s]".formatted(key, value));
                    continue;
                }
                normalized.add(value);
                continue;
            }

            if (!seen.add(value)) {
                warn(source, "Metadata key [%s] contains duplicate identifier [%s]".formatted(key, value));
                continue;
            }
            normalized.add(value);
        }
        return List.copyOf(normalized);
    }

    /**
     * Reads and validates declared plugin dependencies.
     *
     * @param node   manifest root node
     * @param source diagnostic source description
     * @return immutable map of dependency identifiers to version constraints
     * @throws SerializationException if the dependency section is malformed
     */
    private Map<String, String> readDependencies(ConfigurationNode node, String source) throws SerializationException {
        ConfigurationNode dependenciesNode = node.node("dependencies");
        if (dependenciesNode.virtual() || dependenciesNode.isNull()) {
            return Map.of();
        }
        if (!dependenciesNode.isMap()) {
            throw new SerializationException(
                    source + ": Metadata key [dependencies] must be a map of plugin identifiers to version constraints"
            );
        }

        Map<String, String> dependencies = new LinkedHashMap<>();
        for (Map.Entry<Object, ? extends ConfigurationNode> entry : dependenciesNode.childrenMap().entrySet()) {
            String dependencyId = String.valueOf(entry.getKey()).strip();
            if (dependencyId.isEmpty()) {
                throw new SerializationException(source + ": Dependency identifier cannot be blank");
            }

            ConfigurationNode valueNode = entry.getValue();
            String versionConstraint;
            if (valueNode.virtual() || valueNode.isNull()) {
                versionConstraint = "*";
            } else {
                if (valueNode.isMap() || valueNode.isList()) {
                    throw new SerializationException(
                            source + ": Dependency [" + dependencyId + "] must declare a version constraint string"
                    );
                }
                versionConstraint = valueNode.getString();
                if (versionConstraint == null) {
                    versionConstraint = "*";
                }
                versionConstraint = versionConstraint.strip();
                if (versionConstraint.isEmpty()) {
                    throw new SerializationException(
                            source + ": Dependency [" + dependencyId + "] has a blank version constraint"
                    );
                }
            }

            if (dependencies.putIfAbsent(dependencyId, versionConstraint) != null) {
                throw new SerializationException(source + ": Duplicate dependency identifier [" + dependencyId + "]");
            }
        }
        return Map.copyOf(dependencies);
    }

    /**
     * Reads and validates the metadata schema version.
     *
     * @param node   manifest root node
     * @param source diagnostic source description
     * @return declared schema version
     * @throws SerializationException if the schema value is missing or malformed
     */
    private int readSchema(ConfigurationNode node, String source) throws SerializationException {
        ConfigurationNode schemaNode = node.node("schema");
        if (schemaNode.virtual() || schemaNode.isNull()) {
            throw new SerializationException(source + ": Missing required metadata key [schema]");
        }

        if (schemaNode.isMap() || schemaNode.isList()) {
            throw new SerializationException(source + ": Metadata key [schema] must be an integer");
        }

        String rawValue = schemaNode.getString();
        if (rawValue == null || rawValue.isBlank()) {
            throw new SerializationException(source + ": Missing required metadata key [schema]");
        }

        try {
            return Integer.parseInt(rawValue.strip());
        } catch (NumberFormatException e) {
            throw withCause(source + ": Metadata key [schema] must be an integer", e);
        }
    }

    /**
     * Reads and validates the target execution environment.
     *
     * @param node   manifest root node
     * @param source diagnostic source description
     * @return declared environment, or {@link Environment#BOTH} when omitted
     * @throws SerializationException if the environment value is malformed or strict validation rejects an
     *                                unknown value
     */
    private Environment readEnvironment(ConfigurationNode node, String source) throws SerializationException {
        ConfigurationNode environmentNode = node.node("environment");
        if (environmentNode.virtual() || environmentNode.isNull()) {
            return Environment.BOTH;
        }

        if (environmentNode.isMap() || environmentNode.isList()) {
            throw new SerializationException(source + ": Metadata key [environment] must be a string");
        }

        String rawValue = environmentNode.getString();
        if (rawValue == null || rawValue.isBlank()) {
            return Environment.BOTH;
        }

        String normalized = rawValue.strip().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "client" -> Environment.CLIENT;
            case "server" -> Environment.SERVER;
            case "both", "*" -> Environment.BOTH;
            default -> {
                if (strict) {
                    throw new SerializationException(source + ": Unknown metadata environment [" + rawValue + "]");
                }
                warn(source, "Unknown metadata environment [" + rawValue + "]; assuming [both]");
                yield Environment.BOTH;
            }
        };
    }

    /**
     * Reports manifest keys that are not part of the supported schema.
     *
     * @param node   manifest root node
     * @param source diagnostic source description
     */
    private void validateTopLevelKeys(ConfigurationNode node, String source) throws SerializationException {
        for (Object key : node.childrenMap().keySet()) {
            String keyName = String.valueOf(key);
            if (!KNOWN_KEYS.contains(keyName)) {
                warn(source, "Unknown metadata key [" + keyName + "]");
            }
        }
    }

    /**
     * Requires duplicate-key rejection on Configurate's embedded YAML parser.
     * <p>
     * Configurate exposes no public loader option for this check. An incompatible internal layout is a parsing
     * failure, never a reason to silently accept duplicate manifest keys.
     *
     * @param loader loader to configure
     * @throws SerializationException if the required option cannot be enabled and verified
     */
    private static void enableDuplicateKeyDetection(YamlConfigurationLoader loader) throws SerializationException {
        try {
            Object yamlHolder = readField(loader, "yaml");
            if (!(yamlHolder instanceof ThreadLocal<?> threadLocal)) {
                throw new IllegalStateException("YAML parser holder is not a ThreadLocal");
            }
            Object yaml = Objects.requireNonNull(threadLocal.get(), "YAML parser is unavailable");
            Object constructor = readField(yaml, "constructor");
            Field option = findField(constructor.getClass(), "allowDuplicateKeys");
            option.setAccessible(true);
            option.setBoolean(constructor, false);
            if (option.getBoolean(constructor)) {
                throw new IllegalStateException("Duplicate YAML keys remain enabled");
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            throw withCause("Required duplicate YAML key detection is unavailable", e);
        }
    }

    private static Object readField(Object target, String name) throws ReflectiveOperationException {
        Field field = findField(target.getClass(), name);
        field.setAccessible(true);
        return field.get(target);
    }

    private static Field findField(Class<?> type, String name) throws NoSuchFieldException {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            try {
                return current.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                // The embedded YAML constructor may inherit its duplicate-key option.
            }
        }
        throw new NoSuchFieldException(name);
    }

    /**
     * Extracts the most useful message from a parsing failure.
     *
     * @param failure parsing failure
     * @return cause message when available, otherwise the failure message or class name
     */
    private static String parseFailureMessage(Throwable failure) {
        Throwable cause = failure.getCause();
        if (cause != null && cause.getMessage() != null && !cause.getMessage().isBlank()) {
            return cause.getMessage();
        }
        return failure.getMessage() == null ? failure.getClass().getSimpleName() : failure.getMessage();
    }

    /**
     * Creates a serialization exception that preserves the original cause.
     *
     * @param message diagnostic message
     * @param cause   original failure
     * @return serialization exception with the provided message and cause
     */
    private static SerializationException withCause(String message, Throwable cause) {
        SerializationException exception = new SerializationException(message);
        exception.initCause(cause);
        return exception;
    }

    /**
     * Emits a non-fatal metadata warning.
     *
     * @param source  diagnostic source description
     * @param message diagnostic message
     */
    private void warn(String source, String message) {
        LOGGER.warn(source + ": " + message);
    }

    /**
     * Class-name declarations grouped by side.
     *
     * @param shared common declarations; main entrypoints for the entrypoint section, common mixins for the mixin
     *               section
     * @param client client-only declarations
     * @param server server-only declarations
     */
    private record ClassSections(List<String> shared, List<String> client, List<String> server) {
    }

    /**
     * Parsed plugin manifest together with archive metadata from the same open JAR.
     *
     * @param metadata     parsed manifest descriptor
     * @param classEntries immutable list of class resource paths
     * @param multiRelease whether the archive is a multi-release JAR
     * @param iconPresent  whether the archive contains the configured plugin icon
     */
    public record ParsedJarManifest(
            Metadata metadata,
            List<String> classEntries,
            boolean multiRelease,
            boolean iconPresent
    ) {

        /**
         * Creates a manifest descriptor without archive feature flags.
         *
         * @param metadata     parsed manifest descriptor
         * @param classEntries immutable list of class resource paths
         */
        public ParsedJarManifest(Metadata metadata, List<String> classEntries) {
            this(metadata, classEntries, false, false);
        }

        /**
         * Validates and defensively copies the parsed manifest data.
         *
         * @param metadata     parsed manifest descriptor
         * @param classEntries immutable list of class resource paths
         * @param multiRelease whether the archive is a multi-release JAR
         * @param iconPresent  whether the archive contains the configured plugin icon
         * @throws NullPointerException if {@code metadata} or {@code classEntries} is {@code null}
         */
        public ParsedJarManifest {
            Objects.requireNonNull(metadata, "Metadata cannot be null");
            classEntries = List.copyOf(classEntries);
        }
    }
}
