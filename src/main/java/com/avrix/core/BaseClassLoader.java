package com.avrix.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Custom {@link URLClassLoader} with dedicated native library path resolution
 * and automatic URL deduplication. Thread-safe for concurrent registration.
 */
public class BaseClassLoader extends URLClassLoader {
    private static final Logger log = LoggerFactory.getLogger(BaseClassLoader.class);

    /**
     * Directories scanned by {@link #findLibrary(String)}.
     */
    private final Set<Path> nativePaths = ConcurrentHashMap.newKeySet();

    /**
     * Normalized URI strings used to prevent duplicate URL registration.
     */
    private final Set<String> urlKeys = ConcurrentHashMap.newKeySet();

    /**
     * Creates a new classloader with the specified URLs and parent.
     * URLs are added via {@link #addURL(URL)} to enforce deduplication.
     */
    public BaseClassLoader(URL[] urls, ClassLoader parent) {
        super(new URL[0], Objects.requireNonNull(parent, "Parent classloader must not be null"));
        Objects.requireNonNull(urls, "URLs must not be null");
        for (URL url : urls) {
            addURL(url);
        }
    }

    /**
     * Resolves a native library by searching {@link #nativePaths}.
     * Falls back to standard {@link URLClassLoader} resolution if not found locally.
     */
    @Override
    protected String findLibrary(String libname) {
        Objects.requireNonNull(libname, "Library name must not be null");
        String mapped = System.mapLibraryName(libname);

        for (Path dir : nativePaths) {
            Path candidate = dir.resolve(mapped);
            if (Files.isRegularFile(candidate)) {
                Path abs = candidate.toAbsolutePath().normalize();
                log.trace("Library '{}' found at '{}'", libname, abs);
                return abs.toString();
            }
        }

        String fallback = super.findLibrary(libname);
        if (fallback != null) {
            log.trace("Library '{}' resolved via parent/URLs: '{}'", libname, fallback);
            return fallback;
        }

        log.warn("Library '{}' not found in any registered native path.", libname);
        return null;
    }

    /**
     * Adds a URL to the classpath. Duplicate URLs (by normalized URI) are ignored.
     */
    @Override
    public void addURL(URL url) {
        Objects.requireNonNull(url, "URL must not be null");
        final String key;
        try {
            key = url.toURI().normalize().toString();
        } catch (URISyntaxException e) {
            log.error("Invalid URI syntax for URL '{}'.", url, e);
            throw new IllegalArgumentException("Invalid URI syntax in the provided URL: " + url, e);
        }

        if (!urlKeys.add(key)) {
            log.trace("URL '{}' is already added. Skipping.", displayName(url));
            return;
        }

        super.addURL(url);
        log.trace("Added URL to classloader: '{}'", displayName(url));
    }

    /**
     * Registers a directory for native library resolution.
     *
     * @throws IllegalArgumentException if the path is not an existing directory
     */
    public void addNativePath(Path path) {
        Objects.requireNonNull(path, "Native path must not be null");
        Path normalized = path.toAbsolutePath().normalize();
        if (!Files.isDirectory(normalized)) {
            throw new IllegalArgumentException("Native path must be an existing directory: " + normalized);
        }
        if (nativePaths.add(normalized)) {
            log.trace("Native path added: '{}'", normalized);
        } else {
            log.trace("Native path already exists: '{}'", normalized);
        }
    }

    /**
     * Batch-registers directories for native library resolution.
     */
    public void addNativePaths(Collection<Path> paths) {
        Objects.requireNonNull(paths, "Paths collection must not be null");
        for (Path p : paths) {
            addNativePath(p);
        }
    }

    /**
     * Extracts a concise file name from a URL for logging.
     * Falls back to {@link URL#toExternalForm()} on parsing failure.
     */
    private static String displayName(URL url) {
        try {
            var uri = url.toURI();
            if ("file".equalsIgnoreCase(uri.getScheme()) && uri.isAbsolute()) {
                Path p = Paths.get(uri);
                Path fileName = p.getFileName();
                if (fileName != null) {
                    return fileName.toString();
                }
            }
        } catch (URISyntaxException | IllegalArgumentException ignored) {
            // Fallback to external form
        }
        return url.toExternalForm();
    }
}