package com.avrix.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Properties;

/**
 * Shared reader for the packaged {@code avrix.properties} build metadata.
 *
 * <p><b>API Note:</b> This is a stateless utility class and cannot be instantiated.
 */
public final class LoaderProperties {

    /**
     * Classpath resource name containing loader build metadata.
     */
    public static final String RESOURCE_NAME = "avrix.properties";

    /**
     * Prevents instantiation of this utility class.
     *
     * @throws UnsupportedOperationException always
     */
    private LoaderProperties() {
        throw new UnsupportedOperationException("LoaderProperties utility class cannot be instantiated");
    }

    /**
     * Reads the loader properties from the provided class loader using UTF-8.
     *
     * @param classLoader class loader used to locate the resource, cannot be {@code null}
     * @return loaded properties, or {@code null} when the resource is absent
     * @throws NullPointerException if {@code classLoader} is {@code null}
     * @throws IOException          if the resource cannot be read or parsed
     */
    public static Properties read(ClassLoader classLoader) throws IOException {
        Objects.requireNonNull(classLoader, "ClassLoader cannot be null");

        try (InputStream stream = classLoader.getResourceAsStream(RESOURCE_NAME)) {
            if (stream == null) {
                return null;
            }
            return load(stream);
        }
    }

    /**
     * Loads properties from the provided stream using UTF-8.
     *
     * @param stream source stream, cannot be {@code null}
     * @return loaded properties
     * @throws NullPointerException if {@code stream} is {@code null}
     * @throws IOException          if the stream cannot be read or parsed
     */
    public static Properties load(InputStream stream) throws IOException {
        Objects.requireNonNull(stream, "InputStream cannot be null");

        Properties properties = new Properties();
        properties.load(new InputStreamReader(stream, StandardCharsets.UTF_8));
        return properties;
    }
}
