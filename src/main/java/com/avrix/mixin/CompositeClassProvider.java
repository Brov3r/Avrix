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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
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
 * @apiNote Implements {@link IClassProvider} for {@link net.lenni0451.classtransform.TransformerManager}.
 */
public final class CompositeClassProvider implements IClassProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(CompositeClassProvider.class);
    private static final ClassLoader PLATFORM_CLASS_LOADER = ClassLoader.getPlatformClassLoader();

    private final KnotClassLoader knotClassLoader;

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
     * Obtains the raw bytecode for the specified class by querying the flat classloader and platform runtime.
     *
     * @param name the fully qualified binary name of the class (e.g., {@code zombie.characters.IsoPlayer})
     * @return the raw bytecode array
     * @throws NullPointerException     if {@code name} is null
     * @throws IllegalArgumentException if the class bytecode could not be found
     */
    @Override
    public byte[] getClass(String name) {
        Objects.requireNonNull(name, "Class name cannot be null");
        String resourcePath = name.replace('.', '/').concat(".class");

        // Primary lookup in unified KnotClassLoader (Game + Loader Core + All Plugins)
        byte[] bytes = readResourceBytes(knotClassLoader, resourcePath);
        if (bytes != null) {
            return bytes;
        }

        // Lookup in Platform ClassLoader (JDK core classes: java.base, java.desktop, etc.)
        bytes = readResourceBytes(PLATFORM_CLASS_LOADER, resourcePath);
        if (bytes != null) {
            return bytes;
        }

        // Fallback to system resource stream
        try (InputStream is = ClassLoader.getSystemResourceAsStream(resourcePath)) {
            if (is != null) {
                return is.readAllBytes();
            }
        } catch (IOException e) {
            LOGGER.trace("Failed to read system resource for class: [{}]", name, e);
        }

        throw new IllegalArgumentException("Bytecode not found for class: " + name);
    }

    /**
     * Scans and returns all discoverable classes mapped to their bytecode suppliers.
     *
     * @return an unmodifiable map of class names to bytecode suppliers
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
                        try (JarFile jarFile = jarConnection.getJarFile()) {
                            scanJarFileEntries(jarFile, classMap);
                        }
                    }
                }
            } catch (Exception e) {
                LOGGER.debug("Failed to scan classpath URL: [{}]", url, e);
            }
        }

        return Collections.unmodifiableMap(classMap);
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
                        String className = resourcePath.substring(0, resourcePath.length() - 6).replace('/', '.');

                        targetMap.putIfAbsent(className, () -> readResourceBytes(knotClassLoader, resourcePath));
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
        try (JarFile jarFile = new JarFile(file)) {
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
        var entries = jarFile.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            String entryName = entry.getName();

            if (!entry.isDirectory() && entryName.endsWith(".class") && !entryName.startsWith("META-INF/")) {
                String className = entryName.substring(0, entryName.length() - 6).replace('/', '.');
                targetMap.putIfAbsent(className, () -> readResourceBytes(knotClassLoader, entryName));
            }
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

        try (InputStream is = loader.getResourceAsStream(resourcePath)) {
            if (is != null) {
                return is.readAllBytes();
            }
        } catch (Exception e) {
            LOGGER.trace("Unable to read resource [{}] from loader [{}]", resourcePath, loader, e);
        }
        return null;
    }
}