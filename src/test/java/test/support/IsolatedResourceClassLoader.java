package test.support;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Child-first class loader that isolates a single class and controls one classpath resource lookup.
 * <p>
 * The isolated class is redefined from the bytes of its parent-loaded counterpart, while every other class is
 * delegated to the parent. This makes it possible to exercise static initialization and resource fallback branches
 * without changing the test classpath.
 */
public final class IsolatedResourceClassLoader extends ClassLoader {

    private final String isolatedClassName;
    private final String resourceName;
    private final String resourceContent;
    private final boolean failOnRead;

    /**
     * Creates a class loader that isolates the given type and controls the specified resource.
     *
     * @param isolatedType    type to redefine in this loader
     * @param resourceName    exact resource name whose lookup is intercepted (without a leading slash)
     * @param resourceContent resource content to serve, or {@code null} to hide the resource
     * @param failOnRead      whether the resource stream should fail while being read
     */
    public IsolatedResourceClassLoader(
            Class<?> isolatedType,
            String resourceName,
            String resourceContent,
            boolean failOnRead
    ) {
        super(isolatedType.getClassLoader());
        this.isolatedClassName = isolatedType.getName();
        this.resourceName = resourceName;
        this.resourceContent = resourceContent;
        this.failOnRead = failOnRead;
    }

    /**
     * Loads the isolated type into this class loader.
     *
     * @return isolated class definition
     * @throws ClassNotFoundException if the isolated class bytes are unavailable
     */
    public Class<?> loadIsolatedClass() throws ClassNotFoundException {
        return loadClass(isolatedClassName, true);
    }

    /**
     * Defines the isolated type locally and delegates every other class to the parent.
     *
     * @param name    binary class name
     * @param resolve whether to resolve the class after loading
     * @return loaded class
     * @throws ClassNotFoundException if the class cannot be loaded
     */
    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        if (!isolatedClassName.equals(name)) {
            return super.loadClass(name, resolve);
        }

        synchronized (getClassLoadingLock(name)) {
            Class<?> loaded = findLoadedClass(name);
            if (loaded == null) {
                loaded = defineFromParent(name);
            }
            if (resolve) {
                resolveClass(loaded);
            }
            return loaded;
        }
    }

    /**
     * Reads the class bytes from the parent loader and defines them in this loader.
     *
     * @param name binary class name
     * @return locally defined class
     * @throws ClassNotFoundException if the parent cannot provide the class bytes
     */
    private Class<?> defineFromParent(String name) throws ClassNotFoundException {
        String classResource = name.replace('.', '/') + ".class";

        try (InputStream stream = getParent().getResourceAsStream(classResource)) {
            if (stream == null) {
                throw new ClassNotFoundException(name);
            }
            byte[] bytes = stream.readAllBytes();
            return defineClass(name, bytes, 0, bytes.length);
        } catch (IOException e) {
            throw new ClassNotFoundException(name, e);
        }
    }

    /**
     * Serves the configured resource content, hides it, or returns a failing stream.
     *
     * @param name resource name
     * @return controlled resource stream
     */
    @Override
    public InputStream getResourceAsStream(String name) {
        if (!resourceName.equals(name)) {
            return super.getResourceAsStream(name);
        }
        if (failOnRead) {
            return new FailingInputStream();
        }
        if (resourceContent == null) {
            return null;
        }
        return new ByteArrayInputStream(resourceContent.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Stream that fails on the first read attempt.
     */
    private static final class FailingInputStream extends InputStream {

        /**
         * Simulates an unreadable resource.
         *
         * @return never returns normally
         * @throws IOException always
         */
        @Override
        public int read() throws IOException {
            throw new IOException("Simulated unreadable resource");
        }

        /**
         * Simulates an unreadable resource.
         *
         * @param bytes  destination buffer
         * @param offset destination offset
         * @param length maximum number of bytes to read
         * @return never returns normally
         * @throws IOException always
         */
        @Override
        public int read(byte[] bytes, int offset, int length) throws IOException {
            throw new IOException("Simulated unreadable resource");
        }
    }
}
