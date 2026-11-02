package com.avrix;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the private command-line filtering logic in {@link Main}.
 */
@DisplayName("Main Argument Filtering Unit Tests")
class MainArgumentsTest {

    private static final Method FILTER_LOADER_ARGUMENTS = findFilterMethod();

    @Test
    @DisplayName("Should keep game arguments unchanged")
    void shouldKeepGameArguments() throws Exception {
        String[] result = filter("-game", "--data", "value");

        assertThat(result).containsExactly("-game", "--data", "value");
    }

    @Test
    @DisplayName("Should remove loader-specific flags")
    void shouldRemoveLoaderSpecificFlags() throws Exception {
        String[] result = filter("--no-redirect-log", "-game", "--strict", "--data", "value");

        assertThat(result).containsExactly("-game", "--data", "value");
    }

    @Test
    @DisplayName("Should return an empty array for empty input")
    void shouldReturnEmptyArrayForEmptyInput() throws Exception {
        String[] result = filter();

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should return an empty array when only loader flags are present")
    void shouldReturnEmptyArrayForOnlyLoaderFlags() throws Exception {
        String[] result = filter("--no-redirect-log", "--strict");

        assertThat(result).isEmpty();
    }

    private static String[] filter(String... args) throws Exception {
        return (String[]) FILTER_LOADER_ARGUMENTS.invoke(null, (Object) args);
    }

    private static Method findFilterMethod() {
        try {
            Method method = Main.class.getDeclaredMethod("filterLoaderArguments", String[].class);
            method.setAccessible(true);
            return method;
        } catch (NoSuchMethodException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }
}
