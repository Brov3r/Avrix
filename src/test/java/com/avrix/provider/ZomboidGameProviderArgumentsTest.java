package com.avrix.provider;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for Project Zomboid launch-argument handling.
 */
@DisplayName("ZomboidGameProvider Argument Unit Tests")
class ZomboidGameProviderArgumentsTest {

    @TempDir
    Path tempDir;

    private String originalCacheDirectory;
    private String originalServerName;

    @BeforeEach
    void setUp() {
        originalCacheDirectory = System.getProperty(ZomboidConstants.CACHE_DIR_PROPERTY);
        originalServerName = System.getProperty(ZomboidConstants.SERVER_NAME_PROPERTY);
        System.clearProperty(ZomboidConstants.CACHE_DIR_PROPERTY);
        System.clearProperty(ZomboidConstants.SERVER_NAME_PROPERTY);
    }

    @AfterEach
    void tearDown() {
        restoreProperty(ZomboidConstants.CACHE_DIR_PROPERTY, originalCacheDirectory);
        restoreProperty(ZomboidConstants.SERVER_NAME_PROPERTY, originalServerName);
    }

    @Test
    @DisplayName("Should parse and trim the cache directory override")
    void shouldParseCacheDirectory() {
        ZomboidGameProvider provider = new ZomboidGameProvider();
        Path cacheRoot = tempDir.resolve("PZ Data");

        provider.configure(new String[]{"-game", "-cachedir=  " + cacheRoot + "  ", "-debug"});

        assertThat(provider.getCacheRoot()).isEqualTo(cacheRoot.toAbsolutePath().normalize());
    }

    @Test
    @DisplayName("Should keep the last cache directory override")
    void shouldKeepLastCacheDirectory() {
        ZomboidGameProvider provider = new ZomboidGameProvider();

        provider.configure(new String[]{"-cachedir=first", "-cachedir=second"});

        assertThat(provider.getCacheRoot()).isEqualTo(Path.of("second").toAbsolutePath().normalize());
    }

    @Test
    @DisplayName("Should parse the server name in both supported forms")
    void shouldParseServerName() {
        ZomboidGameProvider provider = new ZomboidGameProvider();

        provider.configure(new String[]{"-servername", "first-server"});
        assertThat(System.getProperty(ZomboidConstants.SERVER_NAME_PROPERTY)).isEqualTo("first-server");

        provider.configure(new String[]{"-servername=second-server"});
        assertThat(System.getProperty(ZomboidConstants.SERVER_NAME_PROPERTY)).isEqualTo("second-server");
    }

    @Test
    @DisplayName("Should ignore null and unrelated arguments")
    void shouldIgnoreUnrelatedArguments() {
        ZomboidGameProvider provider = new ZomboidGameProvider();

        provider.configure(new String[]{null, "-game", "--data"});

        assertThat(System.getProperty(ZomboidConstants.SERVER_NAME_PROPERTY)).isNull();
        assertThat(System.getProperty(ZomboidConstants.CACHE_DIR_PROPERTY)).isNull();
    }

    private static void restoreProperty(String name, String value) {
        if (value == null) {
            System.clearProperty(name);
        } else {
            System.setProperty(name, value);
        }
    }
}
