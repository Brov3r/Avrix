package com.avrix.plugins;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Caches plugin archive entry names and small resource payloads.
 *
 * <p>The cache is keyed by archive path and invalidated when the archive size or modification time changes.
 * Large resources are read directly and are not retained.
 */
final class PluginResourceCache {

    private static final long MAX_CACHED_RESOURCE_BYTES = 512L * 1024L;
    private static final int MAX_CACHED_RESOURCES = 128;
    private static final ConcurrentMap<Path, ArchiveIndex> INDEX_CACHE = new ConcurrentHashMap<>();
    private static final Map<ResourceKey, byte[]> RESOURCE_CACHE = Collections.synchronizedMap(
            new LinkedHashMap<>(64, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<ResourceKey, byte[]> eldest) {
                    return size() > MAX_CACHED_RESOURCES;
                }
            }
    );

    /**
     * Prevents instantiation of this utility class.
     *
     * @throws UnsupportedOperationException always
     */
    private PluginResourceCache() {
        throw new UnsupportedOperationException("PluginResourceCache utility class cannot be instantiated");
    }

    /**
     * Checks whether a JAR contains a regular file entry.
     *
     * @param jarPath     archive path
     * @param entryName   entry name
     * @return {@code true} when the entry exists and is not a directory
     * @throws IOException if the archive cannot be read
     */
    static boolean containsEntry(Path jarPath, String entryName) throws IOException {
        return index(jarPath).entries().contains(entryName);
    }

    /**
     * Returns all non-directory entry names in JAR order.
     *
     * @param jarPath archive path
     * @return immutable list of entry names
     * @throws IOException if the archive cannot be read
     */
    static List<String> entryNames(Path jarPath) throws IOException {
        return index(jarPath).entries();
    }

    /**
     * Reads a resource, using the bounded payload cache for small files.
     *
     * @param jarPath   archive path
     * @param entryName entry name
     * @return resource bytes, or empty when the entry is absent
     * @throws IOException if the archive cannot be read
     */
    static Optional<byte[]> read(Path jarPath, String entryName) throws IOException {
        final long size;
        final FileTime modified;
        try {
            BasicFileAttributes attributes = Files.readAttributes(jarPath, BasicFileAttributes.class);
            size = attributes.size();
            modified = attributes.lastModifiedTime();
        } catch (FileNotFoundException | NoSuchFileException e) {
            return Optional.empty();
        }

        ResourceKey cacheKey = new ResourceKey(jarPath, entryName, modified, size);
        byte[] cached = RESOURCE_CACHE.get(cacheKey);
        if (cached != null) {
            return Optional.of(cached.clone());
        }

        try (JarFile jar = new JarFile(jarPath.toFile(), false)) {
            JarEntry entry = jar.getJarEntry(entryName);
            if (entry == null || entry.isDirectory()) {
                return Optional.empty();
            }

            try (InputStream input = jar.getInputStream(entry)) {
                byte[] bytes = input.readAllBytes();
                if (bytes.length <= MAX_CACHED_RESOURCE_BYTES) {
                    RESOURCE_CACHE.put(cacheKey, bytes.clone());
                }
                return Optional.of(bytes);
            }
        } catch (FileNotFoundException e) {
            return Optional.empty();
        }
    }

    private static ArchiveIndex index(Path jarPath) throws IOException {
        BasicFileAttributes attributes = Files.readAttributes(jarPath, BasicFileAttributes.class);
        long size = attributes.size();
        FileTime modified = attributes.lastModifiedTime();
        ArchiveIndex cached = INDEX_CACHE.get(jarPath);
        if (cached != null && cached.size() == size && cached.modified().equals(modified)) {
            return cached;
        }

        List<String> entries = new ArrayList<>();
        try (JarFile jar = new JarFile(jarPath.toFile(), false)) {
            var enumeration = jar.entries();
            while (enumeration.hasMoreElements()) {
                JarEntry entry = enumeration.nextElement();
                if (!entry.isDirectory()) {
                    entries.add(entry.getName());
                }
            }
        }

        ArchiveIndex index = new ArchiveIndex(size, modified, List.copyOf(entries));
        INDEX_CACHE.put(jarPath, index);
        return index;
    }

    private record ArchiveIndex(long size, FileTime modified, List<String> entries) {
    }

    private record ResourceKey(Path jarPath, String entryName, FileTime modified, long size) {
    }
}
