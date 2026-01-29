package com.avrix.loaders;

import org.tinylog.Logger;

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
 * {@code CoreClassLoader} is a specialized {@link URLClassLoader} used by the Avrix runtime
 * to load game-related classes and native libraries in a controlled and deterministic way.
 */
public class AvrixClassLoader extends URLClassLoader {

    /**
     * Registered directories used to search for native libraries.
     * Paths are stored as normalized absolute paths.
     */
    private final Set<Path> nativePaths = ConcurrentHashMap.newKeySet();

    /**
     * Stable deduplication keys for URLs already added to this classloader.
     */
    private final Set<String> urlKeys = ConcurrentHashMap.newKeySet();

    /**
     * Creates a new {@code CoreClassLoader} with the given initial URLs and parent classloader.
     *
     * @param urls   initial classpath URLs; must not be {@code null}
     * @param parent parent classloader; must not be {@code null}
     * @throws NullPointerException if {@code urls} or {@code parent} is {@code null}
     */
    public AvrixClassLoader(URL[] urls, ClassLoader parent) {
        super(new URL[0], Objects.requireNonNull(parent, "Parent classloader must not be null"));

        Objects.requireNonNull(urls, "URLs must not be null");
        for (URL url : urls) {
            addURL(url);
        }
    }

    /**
     * Resolves the absolute path to a native library.
     *
     * <p>
     * The library name is mapped using {@link System#mapLibraryName(String)} and
     * then searched in all registered native paths.
     * </p>
     *
     * @param libname logical library name (without platform-specific prefix/suffix)
     * @return absolute path to the native library, or {@code null} if not found
     * @throws NullPointerException if {@code libname} is {@code null}
     */
    @Override
    protected String findLibrary(String libname) {
        Objects.requireNonNull(libname, "Library name must not be null");

        String mapped = System.mapLibraryName(libname);

        for (Path dir : nativePaths) {
            Path candidate = dir.resolve(mapped);
            if (Files.isRegularFile(candidate)) {
                Path abs = candidate.toAbsolutePath().normalize();
                Logger.trace("Library '{}' found at '{}'", libname, abs);
                return abs.toString();
            }
        }

        Logger.warn("Library '{}' not found in any registered native path.", libname);
        return null;
    }

    /**
     * Adds a URL to this classloader if it has not already been added.
     *
     * <p>
     * Deduplication is based on a normalized URI key, not on object identity.
     * </p>
     *
     * @param url classpath URL to add
     * @throws NullPointerException     if {@code url} is {@code null}
     * @throws IllegalArgumentException if the URL cannot be converted to a valid URI
     */
    @Override
    public void addURL(URL url) {
        Objects.requireNonNull(url, "URL must not be null");

        final String key = toStableKey(url);
        if (!urlKeys.add(key)) {
            Logger.trace("URL '{}' is already added to the classloader. Skipping.", displayName(url));
            return;
        }

        super.addURL(url);
        Logger.trace("Added URL to classloader: '{}'", displayName(url));
    }

    /**
     * Registers a directory to be used for native library lookup.
     *
     * <p>
     * The path is converted to a normalized absolute path and must refer to an existing directory.
     * </p>
     *
     * @param path directory containing native libraries
     * @throws NullPointerException     if {@code path} is {@code null}
     * @throws IllegalArgumentException if {@code path} does not exist or is not a directory
     */
    public void addNativePath(Path path) {
        Objects.requireNonNull(path, "Native path must not be null");

        Path normalized = path.toAbsolutePath().normalize();
        if (!Files.isDirectory(normalized)) {
            throw new IllegalArgumentException("Native path must be an existing directory: " + normalized);
        }

        if (nativePaths.add(normalized)) {
            Logger.trace("Native path added: '{}'", normalized);
        } else {
            Logger.trace("Native path already exists: '{}'", normalized);
        }
    }

    /**
     * Registers multiple directories for native library lookup.
     *
     * @param paths collection of native library directories
     * @throws NullPointerException     if {@code paths} or any element is {@code null}
     * @throws IllegalArgumentException if any path is not a valid directory
     */
    public void addNativePaths(Collection<Path> paths) {
        Objects.requireNonNull(paths, "Paths collection must not be null");
        for (Path p : paths) {
            addNativePath(p);
        }
    }

    /**
     * Converts a URL into a stable, normalized deduplication key.
     *
     * @param url URL to normalize
     * @return normalized URI string
     * @throws IllegalArgumentException if the URL has invalid URI syntax
     */
    private static String toStableKey(URL url) {
        try {
            return url.toURI().normalize().toString();
        } catch (URISyntaxException e) {
            Logger.error("Invalid URI syntax for URL '{}'.", url, e);
            throw new IllegalArgumentException("Invalid URI syntax in the provided URL: " + url, e);
        }
    }

    /**
     * Returns a human-readable display name for logging purposes.
     *
     * @param url URL to format
     * @return file name if available, otherwise the external URL form
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
        } catch (Exception ignored) {
            // Best-effort display name. Intentionally ignored.
        }
        return url.toExternalForm();
    }
}