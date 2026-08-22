package com.avrix.plugins;

import com.avrix.core.Metadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.function.Predicate;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Immutable runtime descriptor and isolated resource provider for an Avrix plugin or game provider.
 * <p>
 * In unified ClassLoader runtimes (e.g., KnotClassLoader), classpath resource lookups are vulnerable
 * to cross-JAR collisions. This record guarantees isolated, deterministic resource resolution directly
 * from the underlying physical JAR archive.
 *
 * @param pluginPath physical path to the plugin JAR file on disk; {@code null} for virtual/synthetic plugins
 * @param iconURI    URI pointing to the plugin icon graphic; {@code null} if omitted
 * @param instance   instantiated {@link Plugin} lifecycle instance; {@code null} for library/mixin-only plugins
 * @param metadata   declarative manifest descriptor; cannot be {@code null}
 */
public record PluginData(
        Path pluginPath,
        URI iconURI,
        Plugin instance,
        Metadata metadata
) {
    private static final Logger LOGGER = LoggerFactory.getLogger(PluginData.class);

    /**
     * Compact constructor enforcing domain invariants and path normalization.
     *
     * @throws NullPointerException if {@code metadata} is {@code null}
     */
    public PluginData {
        Objects.requireNonNull(metadata, "Plugin metadata cannot be null");
        pluginPath = (pluginPath != null) ? pluginPath.toAbsolutePath().normalize() : null;
    }

    /**
     * Secondary constructor supporting legacy {@link File} references.
     *
     * @param pluginFile physical JAR file on disk; can be {@code null}
     * @param iconURI    URI pointing to the plugin icon; can be {@code null}
     * @param instance   plugin instance; can be {@code null}
     * @param metadata   plugin metadata descriptor; cannot be {@code null}
     */
    public PluginData(File pluginFile, URI iconURI, Plugin instance, Metadata metadata) {
        this(pluginFile != null ? pluginFile.toPath() : null, iconURI, instance, metadata);
    }

    /**
     * Secondary constructor for synthetic or core game providers containing only metadata.
     *
     * @param metadata plugin metadata descriptor; cannot be {@code null}
     */
    public PluginData(Metadata metadata) {
        this((Path) null, null, null, metadata);
    }

    /**
     * Returns the unique string identifier of the plugin.
     *
     * @return non-null plugin ID
     */
    public String id() {
        return metadata.id();
    }

    /**
     * Retrieves the physical {@link Path} to the plugin JAR archive.
     *
     * @return optional containing the absolute path, or empty if synthetic
     */
    public Optional<Path> getPluginPath() {
        return Optional.ofNullable(pluginPath);
    }

    /**
     * Retrieves the physical {@link File} of the plugin JAR archive.
     *
     * @return optional containing the file handle, or empty if synthetic
     */
    public Optional<File> getPluginFile() {
        return Optional.ofNullable(pluginPath).map(Path::toFile);
    }

    /**
     * Retrieves the URI pointing to the plugin icon asset.
     *
     * @return optional containing the icon URI, or empty if not provided
     */
    public Optional<URI> getPluginIconURI() {
        return Optional.ofNullable(iconURI);
    }

    /**
     * Retrieves the active {@link Plugin} lifecycle instance.
     *
     * @return optional containing the initialized instance, or empty if library-only
     */
    public Optional<Plugin> getPluginInstance() {
        return Optional.ofNullable(instance);
    }

    /**
     * Checks if this descriptor represents a virtual or synthetic provider without an on-disk JAR.
     *
     * @return {@code true} if synthetic; {@code false} if backed by a physical archive
     */
    public boolean isSynthetic() {
        return pluginPath == null || !Files.isRegularFile(pluginPath);
    }

    /**
     * Checks if a resource exists at the given relative path within the plugin's JAR archive.
     *
     * @param resourcePath relative path to the resource inside the JAR (e.g., {@code "assets/config.json"})
     * @return {@code true} if the resource exists and is a file entry; {@code false} otherwise
     */
    public boolean hasResource(String resourcePath) {
        if (isSynthetic() || resourcePath == null || resourcePath.isBlank()) {
            return false;
        }

        String normalizedPath = normalizeResourcePath(resourcePath);
        if (normalizedPath.isEmpty()) {
            return false;
        }

        try (JarFile jar = new JarFile(pluginPath.toFile())) {
            JarEntry entry = jar.getJarEntry(normalizedPath);
            return entry != null && !entry.isDirectory();
        } catch (IOException e) {
            LOGGER.warn("Failed to inspect resource [{}] in plugin [{}]: {}", normalizedPath, id(), e.getMessage());
            return false;
        }
    }

    /**
     * Opens an in-memory {@link InputStream} for a resource located inside the plugin's JAR archive.
     *
     * @param resourcePath relative path to the resource inside the JAR (e.g., {@code "assets/lang/en_US.json"})
     * @return optional containing an in-memory stream, or empty if not found or synthetic
     * @throws IOException if an unrecoverable I/O error occurs while reading the archive
     */
    public Optional<InputStream> openResource(String resourcePath) throws IOException {
        return readResourceBytes(resourcePath).map(ByteArrayInputStream::new);
    }

    /**
     * Reads all raw bytes of a resource located inside the plugin's JAR archive.
     *
     * @param resourcePath relative path to the resource inside the JAR
     * @return optional containing the raw byte array, or empty if not found
     * @throws IOException if reading the archive fails
     */
    public Optional<byte[]> readResourceBytes(String resourcePath) throws IOException {
        if (isSynthetic() || resourcePath == null || resourcePath.isBlank()) {
            return Optional.empty();
        }

        String normalizedPath = normalizeResourcePath(resourcePath);
        if (normalizedPath.isEmpty()) {
            return Optional.empty();
        }

        try (JarFile jar = new JarFile(pluginPath.toFile())) {
            JarEntry entry = jar.getJarEntry(normalizedPath);
            if (entry == null || entry.isDirectory()) {
                return Optional.empty();
            }

            try (InputStream in = jar.getInputStream(entry)) {
                return Optional.of(in.readAllBytes());
            }
        } catch (FileNotFoundException _) {
            return Optional.empty();
        }
    }

    /**
     * Reads the full text content of a resource using UTF-8 character encoding.
     *
     * @param resourcePath relative path to the resource inside the JAR
     * @return optional containing decoded text, or empty if not found
     * @throws IOException if reading the archive fails
     */
    public Optional<String> readResourceString(String resourcePath) throws IOException {
        return readResourceString(resourcePath, StandardCharsets.UTF_8);
    }

    /**
     * Reads the full text content of a resource using a specified character encoding.
     *
     * @param resourcePath relative path to the resource inside the JAR
     * @param charset      character encoding to decode bytes
     * @return optional containing decoded text, or empty if not found
     * @throws IOException          if reading the archive fails
     * @throws NullPointerException if {@code charset} is {@code null}
     */
    public Optional<String> readResourceString(String resourcePath, Charset charset) throws IOException {
        Objects.requireNonNull(charset, "Charset cannot be null");
        return readResourceBytes(resourcePath).map(bytes -> new String(bytes, charset));
    }

    /**
     * Opens an {@link InputStream} to the plugin's icon graphic from its resolved URI or JAR entry.
     *
     * @return optional containing an input stream for the icon, or empty if no icon is configured
     * @throws IOException if an I/O error occurs opening the stream
     */
    public Optional<InputStream> openIconStream() throws IOException {
        if (iconURI == null) {
            return Optional.empty();
        }
        String scheme = iconURI.getScheme();
        if ("jar".equalsIgnoreCase(scheme) || "file".equalsIgnoreCase(scheme) || "http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)) {
            return Optional.of(iconURI.toURL().openStream());
        }
        return openResource(iconURI.getPath());
    }

    /**
     * Extracts a single resource from the plugin's JAR archive directly to a local filesystem target.
     *
     * @param resourcePath relative path to the resource inside the JAR
     * @param destination  target file path on the local filesystem
     * @param overwrite    whether to replace existing destination files
     * @return {@code true} if the resource was successfully extracted; {@code false} otherwise
     * @throws IOException          if an I/O error occurs during extraction
     * @throws NullPointerException if {@code destination} is {@code null}
     */
    public boolean extractResource(String resourcePath, Path destination, boolean overwrite) throws IOException {
        Objects.requireNonNull(destination, "Destination path cannot be null");
        if (isSynthetic() || resourcePath == null || resourcePath.isBlank()) {
            return false;
        }

        String normalizedPath = normalizeResourcePath(resourcePath);
        if (normalizedPath.isEmpty()) {
            return false;
        }

        try (JarFile jar = new JarFile(pluginPath.toFile())) {
            JarEntry entry = jar.getJarEntry(normalizedPath);
            if (entry == null || entry.isDirectory()) {
                return false;
            }

            Path parent = destination.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            if (!overwrite && Files.exists(destination)) {
                return false;
            }

            try (InputStream in = jar.getInputStream(entry)) {
                Files.copy(in, destination, StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        }
    }

    /**
     * Extracts an entire directory of resources from the JAR archive to a local filesystem directory.
     *
     * @param directoryPath relative path of the source directory inside the JAR (e.g., {@code "media/lua/"})
     * @param targetDir     target directory on the local filesystem
     * @param overwrite     whether to overwrite existing files
     * @return number of files successfully extracted
     * @throws IOException          if directory creation or file copying fails
     * @throws NullPointerException if {@code targetDir} is {@code null}
     */
    public int extractDirectory(String directoryPath, Path targetDir, boolean overwrite) throws IOException {
        Objects.requireNonNull(targetDir, "Target directory cannot be null");
        if (isSynthetic() || directoryPath == null) {
            return 0;
        }

        String normalizedDir = normalizeResourcePath(directoryPath);
        if (!normalizedDir.isEmpty() && !normalizedDir.endsWith("/")) {
            normalizedDir += "/";
        }

        Path normalizedTarget = targetDir.toAbsolutePath().normalize();
        int extractedCount = 0;

        try (JarFile jar = new JarFile(pluginPath.toFile())) {
            var entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();

                if (!name.startsWith(normalizedDir)) {
                    continue;
                }

                String relativePath = name.substring(normalizedDir.length());
                if (relativePath.isBlank()) {
                    continue;
                }

                Path destination = normalizedTarget.resolve(relativePath).normalize();
                if (!destination.startsWith(normalizedTarget)) {
                    LOGGER.warn("Zip Slip vulnerability detected for entry [{}] in plugin [{}]", name, id());
                    continue;
                }

                if (entry.isDirectory()) {
                    Files.createDirectories(destination);
                    continue;
                }

                Path parent = destination.getParent();
                if (parent != null) {
                    Files.createDirectories(parent);
                }

                if (!overwrite && Files.exists(destination)) {
                    continue;
                }

                try (InputStream in = jar.getInputStream(entry)) {
                    Files.copy(in, destination, StandardCopyOption.REPLACE_EXISTING);
                    extractedCount++;
                }
            }
        }
        return extractedCount;
    }

    /**
     * Resolves a standard {@code jar:} scheme URI pointing directly to the resource entry inside the archive.
     *
     * @param resourcePath relative path to the resource inside the JAR
     * @return optional containing the absolute JAR URI, or empty if synthetic or path is invalid
     */
    public Optional<URI> getResourceURI(String resourcePath) {
        if (isSynthetic() || resourcePath == null || resourcePath.isBlank()) {
            return Optional.empty();
        }

        String normalizedPath = normalizeResourcePath(resourcePath);
        if (normalizedPath.isEmpty()) {
            return Optional.empty();
        }

        try {
            URI fileUri = pluginPath.toUri();
            URI pathUri = new URI(null, null, "/" + normalizedPath, null);
            return Optional.of(URI.create("jar:" + fileUri.toASCIIString() + "!" + pathUri.toASCIIString()));
        } catch (URISyntaxException e) {
            LOGGER.warn("Failed to construct URI for resource [{}] in plugin [{}]: {}", resourcePath, id(), e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Lists all resource entry names located inside a given directory inside the plugin's JAR archive.
     *
     * @param directoryPath relative path to the directory inside the JAR (e.g., {@code "assets/mixins/"})
     * @param recursive     whether to traverse nested subdirectories
     * @return unmodifiable list of matching entry paths, or an empty list if not found or synthetic
     */
    public List<String> listResources(String directoryPath, boolean recursive) {
        if (isSynthetic() || directoryPath == null) {
            return List.of();
        }

        String normalizedDir = normalizeResourcePath(directoryPath);
        if (!normalizedDir.isEmpty() && !normalizedDir.endsWith("/")) {
            normalizedDir += "/";
        }

        List<String> results = new ArrayList<>();
        try (JarFile jar = new JarFile(pluginPath.toFile())) {
            var entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();

                if (entry.isDirectory() || !name.startsWith(normalizedDir)) {
                    continue;
                }

                if (!recursive) {
                    String relativePart = name.substring(normalizedDir.length());
                    if (relativePart.contains("/")) {
                        continue;
                    }
                }

                results.add(name);
            }
            return Collections.unmodifiableList(results);
        } catch (IOException e) {
            LOGGER.warn("Failed to list resources in directory [{}] for plugin [{}]: {}", directoryPath, id(), e.getMessage());
            return List.of();
        }
    }

    /**
     * Finds all resource entries matching a specific name/path filter predicate.
     *
     * @param filter predicate tested against each entry path
     * @return unmodifiable list of matching relative entry paths
     */
    public List<String> findResources(Predicate<String> filter) {
        if (isSynthetic() || filter == null) {
            return List.of();
        }

        List<String> results = new ArrayList<>();
        try (JarFile jar = new JarFile(pluginPath.toFile())) {
            var entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (!entry.isDirectory() && filter.test(entry.getName())) {
                    results.add(entry.getName());
                }
            }
            return Collections.unmodifiableList(results);
        } catch (IOException e) {
            LOGGER.warn("Failed to filter resources for plugin [{}]: {}", id(), e.getMessage());
            return List.of();
        }
    }

    /**
     * Sanitizes and normalizes an internal archive resource path against directory traversal attacks.
     *
     * @param path raw resource path
     * @return clean, forward-slash separated path without leading slashes or parent traversals
     */
    private static String normalizeResourcePath(String path) {
        String sanitized = path.replace('\\', '/').strip();
        while (sanitized.startsWith("/")) {
            sanitized = sanitized.substring(1);
        }

        String[] segments = sanitized.split("/");
        List<String> cleanSegments = new ArrayList<>();
        for (String segment : segments) {
            if (segment.isEmpty() || segment.equals(".")) {
                continue;
            }
            if (segment.equals("..")) {
                if (!cleanSegments.isEmpty()) {
                    cleanSegments.removeLast();
                }
            } else {
                cleanSegments.add(segment);
            }
        }
        return String.join("/", cleanSegments);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PluginData that)) return false;
        return Objects.equals(id(), that.id()) && Objects.equals(metadata.version(), that.metadata.version());
    }

    @Override
    public int hashCode() {
        return Objects.hash(id(), metadata.version());
    }

    @Override
    public String toString() {
        return "PluginData[id='%s', version='%s', synthetic=%s]".formatted(
                id(),
                metadata.version(),
                isSynthetic()
        );
    }
}