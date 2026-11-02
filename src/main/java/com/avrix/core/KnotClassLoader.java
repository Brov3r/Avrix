package com.avrix.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSigner;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.Collection;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Unified Flat ClassLoader for the Avrix environment, implementing the Knot architecture.
 * <p>
 * Hosts the core loader, the target game (Project Zomboid), and all external plugin JARs
 * within a single classloading domain. Intercepts class loading to apply bytecode transformations
 * on the fly via {@link ClassTransformer} and resolves native library dependencies.
 *
 * <p><b>API Note:</b> Delegates JVM internals, logging subsystems, and loader infrastructure to parent loader
 * to prevent duplicate class definitions and {@link ClassCastException} across ServiceLoader boundaries.
 */
public class KnotClassLoader extends URLClassLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(KnotClassLoader.class);

    private final Set<Path> nativePaths = ConcurrentHashMap.newKeySet();
    private final Set<String> registeredUrls = ConcurrentHashMap.newKeySet();
    private final Set<String> diagnosedResources = ConcurrentHashMap.newKeySet();
    private final ClassLoadingPolicy loadingPolicy;
    private volatile ClassTransformer classTransformer;

    static {
        registerAsParallelCapable();
    }

    /**
     * Constructs a new {@link KnotClassLoader}.
     *
     * @param urls   initial array of URLs (game JARs, dependencies, plugins)
     * @param parent the parent class loader (typically the bootstrap/system loader)
     * @throws NullPointerException if {@code urls} or {@code parent} is null
     */
    public KnotClassLoader(URL[] urls, ClassLoader parent) {
        this(urls, parent, ClassLoadingPolicy.defaults());
    }

    /**
     * Constructs a new {@link KnotClassLoader} with an explicit classloading policy.
     *
     * @param urls          initial array of URLs (game JARs, dependencies, plugins)
     * @param parent        the parent class loader (typically the bootstrap/system loader)
     * @param loadingPolicy policy used to decide which classes are delegated to the parent
     * @throws NullPointerException if {@code urls}, {@code parent}, or {@code loadingPolicy} is null
     */
    public KnotClassLoader(URL[] urls, ClassLoader parent, ClassLoadingPolicy loadingPolicy) {
        super(new URL[0], Objects.requireNonNull(parent, "Parent ClassLoader must not be null"));
        Objects.requireNonNull(urls, "URLs array must not be null");
        this.loadingPolicy = Objects.requireNonNull(loadingPolicy, "ClassLoadingPolicy must not be null");

        for (URL url : urls) {
            addURL(url);
        }
    }

    /**
     * Sets the bytecode transformer applied to locally defined classes.
     *
     * @param classTransformer transformer to use, or {@code null} to disable transformation
     */
    public void setClassTransformer(ClassTransformer classTransformer) {
        this.classTransformer = classTransformer;
    }

    /**
     * Registers a filesystem directory containing native libraries (.dll, .so, .dylib).
     *
     * @param path directory path to register
     * @throws NullPointerException     if {@code path} is null
     * @throws IllegalArgumentException if {@code path} does not denote an existing directory
     */
    public void addNativePath(Path path) {
        Objects.requireNonNull(path, "Native path must not be null");
        Path normalized = path.toAbsolutePath().normalize();

        if (!Files.isDirectory(normalized)) {
            throw new IllegalArgumentException("Native path must be an existing directory: " + normalized);
        }

        if (nativePaths.add(normalized)) {
            LOGGER.trace("Registered native path: [{}]", normalized);
        }
    }

    /**
     * Registers a collection of native library directories.
     *
     * @param paths collection of directory paths
     * @throws NullPointerException if {@code paths} is null
     */
    public void addNativePaths(Collection<Path> paths) {
        Objects.requireNonNull(paths, "Paths collection must not be null");
        paths.forEach(this::addNativePath);
    }

    /**
     * Resolves a native library by name against the registered native paths.
     * <p>
     * Falls back to the parent class loader when no matching file exists in the registered directories.
     *
     * @param libName platform-independent library name
     * @return absolute path to the resolved library, or {@code null} if no candidate exists
     * @throws NullPointerException if {@code libName} is null
     */
    @Override
    protected String findLibrary(String libName) {
        Objects.requireNonNull(libName, "Library name cannot be null");

        String mappedName = System.mapLibraryName(libName);
        List<String> lookupCandidates = List.of(
                mappedName,
                libName,
                libName + ".dll",
                libName + ".so",
                libName + ".dylib",
                "lib" + libName + ".so",
                "lib" + libName + ".dylib"
        );

        for (Path dir : nativePaths) {
            for (String candidateName : lookupCandidates) {
                Path candidateFile = dir.resolve(candidateName);
                if (Files.isRegularFile(candidateFile)) {
                    Path absolute = candidateFile.toAbsolutePath().normalize();
                    LOGGER.trace("Native library '{}' resolved to [{}]", libName, absolute);
                    return absolute.toString();
                }
            }
        }

        String fallback = super.findLibrary(libName);
        if (fallback != null) {
            LOGGER.trace("Native library '{}' resolved via parent to [{}]", libName, fallback);
            return fallback;
        }

        LOGGER.warn("Native library '{}' could not be resolved in registered paths", libName);
        return null;
    }

    /**
     * Adds a URL to the local classpath unless an equivalent normalized URL is already registered.
     *
     * @param url classpath URL to add
     * @throws NullPointerException     if {@code url} is null
     * @throws IllegalArgumentException if the URL cannot be converted to a valid URI
     */
    @Override
    public void addURL(URL url) {
        Objects.requireNonNull(url, "URL cannot be null");
        final String key;
        try {
            key = url.toURI().normalize().toString();
        } catch (URISyntaxException e) {
            LOGGER.error("Invalid URI syntax for URL '{}'", url, e);
            throw new IllegalArgumentException("Invalid URI syntax in URL: " + url, e);
        }

        if (!registeredUrls.add(key)) {
            LOGGER.trace("URL already registered, skipping: [{}]", formatDisplayName(url));
            return;
        }

        super.addURL(url);
        LOGGER.trace("Added URL to classpath: [{}]", formatDisplayName(url));
    }

    /**
     * Loads a class using local-first resolution with explicit parent delegation for protected prefixes.
     * <p>
     * Classes under the parent-delegated prefixes are loaded by the parent class loader. All other classes
     * are resolved locally first and fall back to the parent when no local definition exists.
     *
     * @param name    binary name of the class
     * @param resolve whether to resolve the class after loading
     * @return the loaded class
     * @throws ClassNotFoundException if the class cannot be found locally or by the parent
     */
    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            // Check if class was already loaded in this ClassLoader
            Class<?> loadedClass = findLoadedClass(name);
            if (loadedClass != null) {
                return loadedClass;
            }

            // Delegate strict JVM runtime and protected loader/logging libraries directly to the parent
            if (loadingPolicy.delegatesToParent(name)) {
                try {
                    return getParent().loadClass(name);
                } catch (ClassNotFoundException _) {
                    // Fall through to local resolution when the parent cannot provide the class.
                }
            }

            // Transform and define class locally in this Knot domain (game classes, plugins, javax.*, etc.)
            try {
                Class<?> localClass = findClass(name);
                if (resolve) {
                    resolveClass(localClass);
                }
                return localClass;
            } catch (ClassNotFoundException _) {
                // Fallback to parent ClassLoader for other platform dependencies
                return super.loadClass(name, resolve);
            }
        }
    }

    /**
     * Finds and defines a class from local classpath resources, applying mixin transformations before definition.
     *
     * @param name binary name of the class
     * @return the defined class
     * @throws ClassNotFoundException if no class resource exists for the given name
     */
    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        String resourcePath = name.replace('.', '/').concat(".class");
        URL resource = findResource(resourcePath);

        if (resource == null) {
            throw new ClassNotFoundException(name);
        }

        try {
            byte[] rawBytes = readResourceBytes(resource);
            byte[] transformedBytes = transformClass(name, rawBytes);
            byte[] finalBytes = (transformedBytes != null) ? transformedBytes : rawBytes;

            definePackageIfNecessary(name);

            URL codeSourceUrl = extractCodeSourceUrl(resource);
            CodeSource codeSource = new CodeSource(codeSourceUrl, (CodeSigner[]) null);
            ProtectionDomain domain = new ProtectionDomain(codeSource, null, this, null);

            return defineClass(name, finalBytes, 0, finalBytes.length, domain);

        } catch (IOException e) {
            throw new ClassNotFoundException("Failed to read bytecode for: " + name, e);
        }
    }

    /**
     * Finds a local resource and reports it when the same path exists in multiple classpath entries.
     *
     * @param name resource path
     * @return first matching local resource, or {@code null} when none exists
     */
    @Override
    public URL findResource(String name) {
        URL resource = super.findResource(name);
        if (resource != null) {
            diagnoseResourceCollisions(name);
        }
        return resource;
    }

    /**
     * Logs a warning when a resource path is present in more than one classpath entry.
     *
     * @param resourcePath resource path to inspect
     */
    private void diagnoseResourceCollisions(String resourcePath) {
        if (!diagnosedResources.add(resourcePath)) {
            return;
        }

        try {
            Enumeration<URL> resources = findResources(resourcePath);
            Set<String> locations = new LinkedHashSet<>();
            while (resources.hasMoreElements()) {
                URL url = resources.nextElement();
                try {
                    locations.add(url.toURI().normalize().toString());
                } catch (URISyntaxException _) {
                    locations.add(url.toExternalForm());
                }
            }

            if (locations.size() > 1) {
                LOGGER.warn("Resource [{}] exists in multiple classpath entries: {}", resourcePath, locations);
            }
        } catch (IOException e) {
            LOGGER.debug("Failed to inspect resource collisions for [{}]", resourcePath, e);
        }
    }

    /**
     * Reads all raw bytes of a class resource without creating persistent file descriptor locks.
     *
     * @param resource the target resource URL
     * @return raw byte array
     * @throws IOException if reading the resource stream fails
     */
    private byte[] readResourceBytes(URL resource) throws IOException {
        URLConnection connection = resource.openConnection();
        connection.setUseCaches(false);

        try (InputStream inputStream = connection.getInputStream();
             ByteArrayOutputStream buffer = new ByteArrayOutputStream(Math.max(4096, inputStream.available()))) {
            inputStream.transferTo(buffer);
            return buffer.toByteArray();
        }
    }

    /**
     * Executes bytecode transformation through the configured {@link ClassTransformer}.
     *
     * @param className binary name of the class
     * @param rawBytes  original raw bytecode
     * @return transformed bytecode, or {@code null} if no modification occurred
     */
    protected byte[] transformClass(String className, byte[] rawBytes) {
        ClassTransformer transformer = this.classTransformer;
        if (transformer == null) {
            return null;
        }

        try {
            byte[] result = transformer.transform(className, rawBytes);

            if (result != null) {
                LOGGER.debug("Transformed class: [{}]", className);
            }
            return result;
        } catch (Throwable t) {
            LOGGER.error("Error occurred while transforming class: [{}]", className, t);
            return null;
        }
    }

    /**
     * Defines the Java package of the class if it has not been defined yet.
     *
     * @param className binary name of the target class
     */
    private void definePackageIfNecessary(String className) {
        int lastDot = className.lastIndexOf('.');
        if (lastDot != -1) {
            String packageName = className.substring(0, lastDot);
            if (getDefinedPackage(packageName) == null) {
                try {
                    definePackage(packageName, null, null, null, null, null, null, null);
                } catch (IllegalArgumentException _) {
                    // Package defined concurrently by another thread
                }
            }
        }
    }

    /**
     * Resolves the archive or folder URL for code source identification.
     *
     * @param resource the resource URL
     * @return the underlying JAR or root directory URL
     */
    private URL extractCodeSourceUrl(URL resource) {
        try {
            URLConnection connection = resource.openConnection();
            connection.setUseCaches(false);
            if (connection instanceof JarURLConnection jarConnection) {
                return jarConnection.getJarFileURL();
            }
        } catch (IOException _) {
        }
        return resource;
    }

    /**
     * Formats a URL for logging purposes.
     *
     * @param url the URL to format
     * @return clean display representation
     */
    private static String formatDisplayName(URL url) {
        try {
            URI uri = url.toURI();
            if ("file".equalsIgnoreCase(uri.getScheme()) && uri.isAbsolute()) {
                Path path = Paths.get(uri);
                Path fileName = path.getFileName();
                if (fileName != null) {
                    return fileName.toString();
                }
            }
        } catch (URISyntaxException | IllegalArgumentException _) {
        }
        return url.toExternalForm();
    }
}