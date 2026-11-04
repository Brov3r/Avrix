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
    private static final Method PARSE_CACHE_DIRECTORY = findCacheDirectoryMethod();

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

    @Test
    @DisplayName("Should parse the Project Zomboid cache directory argument")
    void shouldParseCacheDirectory() throws Exception {
        assertThat(cacheDirectory("-game", "-cachedir=D:\\PZData", "-debug")).isEqualTo("D:\\PZData");
    }

    @Test
    @DisplayName("Should trim the parsed cache directory value")
    void shouldTrimCacheDirectory() throws Exception {
        assertThat(cacheDirectory("-cachedir=  D:\\PZ Data  ")).isEqualTo("D:\\PZ Data");
    }

    @Test
    @DisplayName("Should keep the last cache directory override")
    void shouldKeepLastCacheDirectory() throws Exception {
        assertThat(cacheDirectory("-cachedir=first", "-cachedir=second")).isEqualTo("second");
    }

    @Test
    @DisplayName("Should return null when no cache directory override is present")
    void shouldReturnNullWithoutCacheDirectory() throws Exception {
        assertThat(cacheDirectory("-game", "-debug")).isNull();
    }

    private static String[] filter(String... args) throws Exception {
        return (String[]) FILTER_LOADER_ARGUMENTS.invoke(null, (Object) args);
    }

    private static String cacheDirectory(String... args) throws Exception {
        return (String) PARSE_CACHE_DIRECTORY.invoke(null, (Object) args);
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

    private static Method findCacheDirectoryMethod() {
        try {
            Method method = Main.class.getDeclaredMethod("parseCacheDirectory", String[].class);
            method.setAccessible(true);
            return method;
        } catch (NoSuchMethodException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }
}
