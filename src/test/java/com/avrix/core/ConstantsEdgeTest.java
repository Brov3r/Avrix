package com.avrix.core;

import com.avrix.utils.Constants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import test.support.IsolatedResourceClassLoader;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * Branch-focused unit tests for {@link Constants} static initialization failures.
 */
@DisplayName("Constants Edge Tests")
class ConstantsEdgeTest {

    private static final String PROPERTIES_RESOURCE = "avrix.properties";

    @Test
    @DisplayName("Should fail initialization when the properties resource is missing")
    void shouldFailInitializationWhenResourceIsMissing() {
        IsolatedResourceClassLoader classLoader = new IsolatedResourceClassLoader(
                Constants.class,
                PROPERTIES_RESOURCE,
                null,
                false
        );

        Throwable thrown = catchThrowable(() -> Class.forName(Constants.class.getName(), true, classLoader));

        assertThat(thrown).isInstanceOf(ExceptionInInitializerError.class);
        assertThat(thrown.getCause())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not found in classpath");
    }

    @Test
    @DisplayName("Should fail initialization when the properties resource is unreadable")
    void shouldFailInitializationWhenResourceIsUnreadable() {
        IsolatedResourceClassLoader classLoader = new IsolatedResourceClassLoader(
                Constants.class,
                PROPERTIES_RESOURCE,
                null,
                true
        );

        Throwable thrown = catchThrowable(() -> Class.forName(Constants.class.getName(), true, classLoader));

        assertThat(thrown).isInstanceOf(ExceptionInInitializerError.class);
        assertThat(thrown.getCause()).isInstanceOf(java.io.IOException.class);
    }

    @Test
    @DisplayName("Should populate loader version and name from the packaged properties file")
    void shouldPopulateLoaderVersionAndNameFromProperties() throws Exception {
        Properties properties = new Properties();
        try (InputStream stream = Constants.class.getResourceAsStream("/" + PROPERTIES_RESOURCE)) {
            assertThat(stream).as("avrix.properties must be available on the test classpath").isNotNull();
            properties.load(new InputStreamReader(stream, StandardCharsets.UTF_8));
        }

        assertThat(Constants.LOADER_VERSION).isEqualTo(properties.getProperty("version"));
        assertThat(Constants.LOADER_NAME).isEqualTo(properties.getProperty("projectName"));
    }
}
