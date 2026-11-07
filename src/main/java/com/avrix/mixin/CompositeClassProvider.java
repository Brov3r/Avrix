package com.avrix.mixin;

import com.avrix.core.KnotClassLoader;
import net.lenni0451.classtransform.utils.tree.IClassProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

/**
 * Composite class bytecode provider for the ClassTransform subsystem in the Avrix loader.
 * <p>
 * Queries the unified {@link KnotClassLoader} and the platform runtime classloader
 * to supply raw bytecode for mixin translation and ASM frame computation.
 *
 * <p><b>API Note:</b> Implements {@link IClassProvider} for {@link net.lenni0451.classtransform.TransformerManager}.
 */
public final class CompositeClassProvider implements IClassProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(CompositeClassProvider.class);
    private static final ClassLoader PLATFORM_CLASS_LOADER = ClassLoader.getPlatformClassLoader();

    private final KnotClassLoader knotClassLoader;
    private final Map<String, byte[]> classCache = new ConcurrentHashMap<>();
    private volatile long cachedClasspathVersion = -1L;

    /**
     * Constructs a new {@link CompositeClassProvider}.
     *
     * @param knotClassLoader the unified flat classloader hosting game and plugin classes
     * @throws NullPointerException if {@code knotClassLoader} is null
     */
    public CompositeClassProvider(KnotClassLoader knotClassLoader) {
        this.knotClassLoader = Objects.requireNonNull(knotClassLoader, "KnotClassLoader cannot be null");
    }

    /**
     * Obtains raw bytecode using the flat classloader's parent-delegation policy.
     *
     * @param name the fully qualified binary name of the class (e.g., {@code zombie.characters.IsoPlayer})
     * @return the raw bytecode array
     * @throws NullPointerException     if {@code name} is null
     * @throws ClassNotFoundException   if the class bytecode could not be found
     */
    @Override
    public byte[] getClass(String name) throws ClassNotFoundException {
        Objects.requireNonNull(name, "Class name cannot be null");

        long classpathVersion = knotClassLoader.getClasspathVersion();
        if (classpathVersion != cachedClasspathVersion) {
            classCache.clear();
            cachedClasspathVersion = classpathVersion;
        }

        byte[] cached = classCache.get(name);
        if (cached != null) {
            return cached;
        }

        String resourcePath = name.replace('.', '/').concat(".class");

        boolean parentFirst = knotClassLoader.delegatesToParent(name);
        ClassLoader parent = knotClassLoader.getParent();
        byte[] bytes = parentFirst ? readResourceBytes(parent, resourcePath) : null;
        if (bytes == null) {
            URL localResource = knotClassLoader.findIndexedResource(resourcePath);
            if (localResource == null) {
                localResource = knotClassLoader.findResource(resourcePath);
            }
            bytes = readResourceBytes(localResource);
        }
        if (bytes == null && !parentFirst) {
            bytes = readResourceBytes(parent, resourcePath);
        }
        if (bytes != null) {
            classCache.put(name, bytes);
            return bytes;
        }

        // Lookup in Platform ClassLoader (JDK core classes: java.base, java.desktop, etc.)
        bytes = readResourceBytes(PLATFORM_CLASS_LOADER, resourcePath);
        if (bytes != null) {
            classCache.put(name, bytes);
            return bytes;
        }

        // Fallback to system resource stream
        try (InputStream is = ClassLoader.getSystemResourceAsStream(resourcePath)) {
            if (is != null) {
                byte[] systemBytes = is.readAllBytes();
                classCache.put(name, systemBytes);
                return systemBytes;
            }
        } catch (IOException e) {
            LOGGER.trace("Failed to read system resource for class: [{}]", name, e);
        }

        throw new ClassNotFoundException("Bytecode not found for class: " + name);
    }

    /**
     * Scans and returns all discoverable classes mapped to their bytecode suppliers.
     *
     * <p>The returned map is mutable, as required by the ClassTransform class provider contract.
     * Multi-release JARs expose the entries selected for the running JVM, including version-only classes.
     * Suppliers follow the same delegation policy as {@link #getClass(String)}.
     *
     * @return a mutable map of class names to bytecode suppliers
     */
    @Override
    public Map<String, Supplier<byte[]>> getAllClasses() {
        Map<String, Supplier<byte[]>> classMap = new HashMap<>();

        for (URL url : knotClassLoader.getURLs()) {
            try {
                URI uri = url.toURI();
                String scheme = uri.getScheme();

                if ("file".equalsIgnoreCase(scheme)) {
                    Path path = Paths.get(uri);
                    if (Files.isDirectory(path)) {
                        scanDirectory(path, classMap);
                    } else if (Files.isRegularFile(path) && path.toString().endsWith(".jar")) {
                        scanJarFile(path.toFile(), classMap);
                    }
                } else if ("jar".equalsIgnoreCase(scheme)) {
                    if (url.openConnection() instanceof JarURLConnection jarConnection) {
                        URL jarFileUrl = jarConnection.getJarFileURL();
                        if ("file".equalsIgnoreCase(jarFileUrl.getProtocol())) {
                            scanJarFile(Paths.get(jarFileUrl.toURI()).toFile(), classMap);
                        } else {
                            scanJarFileEntries(jarConnection.getJarFile(), classMap);
                        }
                    }
                }
            } catch (Exception e) {
                LOGGER.debug("Failed to scan classpath URL: [{}]", url, e);
            }
        }

        return classMap;
    }

    /**
     * Recursively traverses a directory structure to discover class files.
     *
     * @param rootDir   the root directory to scan
     * @param targetMap the map collecting class suppliers
     */
    private void scanDirectory(Path rootDir, Map<String, Supplier<byte[]>> targetMap) {
        try (Stream<Path> stream = Files.walk(rootDir)) {
            stream.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".class"))
                    .forEach(path -> {
                        Path relative = rootDir.relativize(path);
                        String resourcePath = relative.toString().replace(File.separatorChar, '/');
                        if ("module-info.class".equals(resourcePath)) {
                            return;
                        }
                        String className = resourcePath.substring(0, resourcePath.length() - 6).replace('/', '.');

                        targetMap.putIfAbsent(className, () -> findClassBytes(className));
                    });
        } catch (IOException e) {
            LOGGER.warn("Failed to traverse classpath directory: [{}]", rootDir, e);
        }
    }

    /**
     * Opens and scans a JAR file on the filesystem.
     *
     * @param file      the JAR file to open
     * @param targetMap the map collecting class suppliers
     */
    private void scanJarFile(File file, Map<String, Supplier<byte[]>> targetMap) {
        try (JarFile jarFile = new JarFile(file, false, JarFile.OPEN_READ, Runtime.version())) {
            scanJarFileEntries(jarFile, targetMap);
        } catch (IOException e) {
            LOGGER.warn("Failed to open JAR file: [{}]", file.getAbsolutePath(), e);
        }
    }

    /**
     * Iterates over entries of an opened {@link JarFile} and registers class bytecode suppliers.
     *
     * @param jarFile   the opened JAR file
     * @param targetMap the map collecting class suppliers
     */
    private void scanJarFileEntries(JarFile jarFile, Map<String, Supplier<byte[]>> targetMap) {
        if (jarFile.isMultiRelease()) {
            jarFile.versionedStream().forEach(entry -> addClassEntry(entry, targetMap));
            return;
        }

        var entries = jarFile.entries();
        while (entries.hasMoreElements()) {
            addClassEntry(entries.nextElement(), targetMap);
        }
    }

    /**
     * Registers a single class entry from an archive as a lazily resolved bytecode supplier.
     *
     * @param entry     archive entry
     * @param targetMap map collecting class suppliers
     */
    private void addClassEntry(JarEntry entry, Map<String, Supplier<byte[]>> targetMap) {
        String entryName = entry.getName();
        if (entry.isDirectory()
                || !entryName.endsWith(".class")
                || entryName.startsWith("META-INF/")
                || "module-info.class".equals(entryName)) {
            return;
        }

        String className = entryName.substring(0, entryName.length() - 6).replace('/', '.');
        targetMap.putIfAbsent(className, () -> findClassBytes(className));
    }

    private byte[] findClassBytes(String className) {
        try {
            return getClass(className);
        } catch (ClassNotFoundException e) {
            LOGGER.trace("Unable to read discovered class [{}]", className, e);
            return null;
        }
    }

    /**
     * Reads bytecode from the specified {@link ClassLoader} without creating locks.
     *
     * @param loader       the class loader to query
     * @param resourcePath the relative resource path
     * @return raw byte array, or {@code null} if not found
     */
    private byte[] readResourceBytes(ClassLoader loader, String resourcePath) {
        if (loader == null) {
            return null;
        }

        try {
            return readResourceBytes(loader.getResource(resourcePath));
        } catch (Exception e) {
            LOGGER.trace("Unable to read resource [{}] from loader [{}]", resourcePath, loader, e);
        }
        return null;
    }

    /**
     * Reads bytecode from a resource URL without creating persistent file locks.
     *
     * @param resource resource URL, may be {@code null}
     * @return raw byte array, or {@code null} if the resource is absent or unreadable
     */
    private byte[] readResourceBytes(URL resource) {
        if (resource == null) {
            return null;
        }

        try {
            URLConnection connection = resource.openConnection();
            connection.setUseCaches(false);
            try (InputStream is = connection.getInputStream()) {
                return is.readAllBytes();
            }
        } catch (IOException e) {
            LOGGER.trace("Unable to read class resource [{}]", resource, e);
            return null;
        }
    }
}
