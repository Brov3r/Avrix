package com.avrix.loaders;

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

public class CoreClassLoader extends URLClassLoader {
    private final Set<Path> nativePaths = ConcurrentHashMap.newKeySet();
    private final Set<String> urlKeys = ConcurrentHashMap.newKeySet();

    private final static Logger log = LoggerFactory.getLogger(CoreClassLoader.class);

    public CoreClassLoader(URL[] urls, ClassLoader parent) {
        super(new URL[0], Objects.requireNonNull(parent, "Parent classloader must not be null"));

        Objects.requireNonNull(urls, "URLs must not be null");

        for (URL url : urls) {
            addURL(url);
        }
    }

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

        log.warn("Library '{}' not found in any registered native path.", libname);
        return null;
    }


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
            log.trace("URL '{}' is already added to the classloader. Skipping.", displayName(url));
            return;
        }

        super.addURL(url);
        log.trace("Added URL to classloader: '{}'", displayName(url));
    }


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


    public void addNativePaths(Collection<Path> paths) {
        Objects.requireNonNull(paths, "Paths collection must not be null");
        for (Path p : paths) {
            addNativePath(p);
        }
    }


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
        }
        return url.toExternalForm();
    }
}