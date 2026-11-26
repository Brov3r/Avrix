package com.avrix.mixin;

import com.avrix.core.KnotClassLoader;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.service.IClassBytecodeProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Composite class bytecode provider for the Mixin subsystem.
 *
 * <p>Reads raw bytecode from the unified {@link KnotClassLoader}, the platform runtime class loader, and the system
 * class path. Lookups follow the flat classloader's parent-delegation policy so Mixin sees the same class versions
 * that the application will define.
 */
public final class CompositeClassProvider implements IClassBytecodeProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(CompositeClassProvider.class);
    private static final ClassLoader PLATFORM_CLASS_LOADER = ClassLoader.getPlatformClassLoader();

    private final Supplier<KnotClassLoader> classLoaderSupplier;
    private final Map<String, byte[]> classCache = new ConcurrentHashMap<>();

    private volatile KnotClassLoader cachedClassLoader;
    private volatile long cachedClasspathVersion = -1L;

    /**
     * Creates a provider bound to a fixed class loader.
     *
     * @param knotClassLoader the unified flat class loader hosting game and plugin classes
     * @throws NullPointerException if {@code knotClassLoader} is null
     */
    public CompositeClassProvider(KnotClassLoader knotClassLoader) {
        this(fixedClassLoader(knotClassLoader));
    }

    private static Supplier<KnotClassLoader> fixedClassLoader(KnotClassLoader classLoader) {
        Objects.requireNonNull(classLoader, "KnotClassLoader cannot be null");
        return () -> classLoader;
    }

    /**
     * Creates a provider that resolves the current class loader for every lookup.
     *
     * @param classLoaderSupplier supplier of the active flat class loader
     * @throws NullPointerException if {@code classLoaderSupplier} is null
     */
    public CompositeClassProvider(Supplier<KnotClassLoader> classLoaderSupplier) {
        this.classLoaderSupplier = Objects.requireNonNull(classLoaderSupplier, "Class loader supplier cannot be null");
    }

    /**
     * Obtains raw bytecode using the flat classloader's parent-delegation policy.
     *
     * @param name the fully qualified binary name of the class
     * @return the raw bytecode array
     * @throws NullPointerException   if {@code name} is null
     * @throws ClassNotFoundException if the class bytecode could not be found
     */
    public byte[] getClass(String name) throws ClassNotFoundException {
        Objects.requireNonNull(name, "Class name cannot be null");

        KnotClassLoader classLoader = classLoaderSupplier.get();
        if (classLoader == null) {
            throw new ClassNotFoundException("No active class loader for: " + name);
        }
        refreshCache(classLoader);

        byte[] cached = classCache.get(name);
        if (cached != null) {
            return cached;
        }

        String resourcePath = name.replace('.', '/').concat(".class");

        boolean parentFirst = classLoader.delegatesToParent(name);
        ClassLoader parent = classLoader.getParent();
        byte[] bytes = parentFirst ? readResourceBytes(parent, resourcePath) : null;
        if (bytes == null) {
            URL localResource = classLoader.findIndexedResource(resourcePath);
            if (localResource == null) {
                localResource = classLoader.findResource(resourcePath);
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

        bytes = readResourceBytes(PLATFORM_CLASS_LOADER, resourcePath);
        if (bytes != null) {
            classCache.put(name, bytes);
            return bytes;
        }

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
     * Reads a class into an ASM node for Mixin processing.
     *
     * @param name the fully qualified binary name of the class
     * @return parsed class node
     * @throws ClassNotFoundException if the bytecode cannot be found
     * @throws IOException            if the class file cannot be read
     */
    @Override
    public ClassNode getClassNode(String name) throws ClassNotFoundException, IOException {
        return getClassNode(name, true, 0);
    }

    /**
     * Reads a class into an ASM node for Mixin processing.
     *
     * @param name            the fully qualified binary name of the class
     * @param runTransformers whether transformer processing is requested
     * @return parsed class node
     * @throws ClassNotFoundException if the bytecode cannot be found
     * @throws IOException            if the class file cannot be read
     */
    @Override
    public ClassNode getClassNode(String name, boolean runTransformers) throws ClassNotFoundException, IOException {
        return getClassNode(name, runTransformers, 0);
    }

    /**
     * Reads a class into an ASM node for Mixin processing.
     *
     * <p>Transformer processing is irrelevant for the Avrix loader because classes are transformed by the owning
     * {@link MixinTransformer} before definition. Reader flags are forwarded to ASM unchanged so Mixin can request
     * reduced parsing such as {@link ClassReader#SKIP_CODE}.
     *
     * @param name           the fully qualified binary name of the class
     * @param runTransformers whether transformer processing is requested
     * @param readerFlags    flags to pass to {@code ClassReader.accept}
     * @return parsed class node
     * @throws ClassNotFoundException if the bytecode cannot be found
     * @throws IOException            if the class file cannot be read
     */
    @Override
    public ClassNode getClassNode(String name, boolean runTransformers, int readerFlags)
            throws ClassNotFoundException, IOException {
        ClassNode classNode = new ClassNode();
        new ClassReader(getClass(name)).accept(classNode, readerFlags);
        return classNode;
    }

    private void refreshCache(KnotClassLoader classLoader) {
        long classpathVersion = classLoader.getClasspathVersion();
        if (classLoader == cachedClassLoader && classpathVersion == cachedClasspathVersion) {
            return;
        }
        synchronized (this) {
            if (classLoader == cachedClassLoader && classpathVersion == cachedClasspathVersion) {
                return;
            }
            classCache.clear();
            cachedClassLoader = classLoader;
            cachedClasspathVersion = classpathVersion;
        }
    }

    /**
     * Reads bytecode from the specified {@link ClassLoader} without creating persistent file locks.
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
