package com.avrix.provider.zomboid;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the cache marker used by Lua for manual and Workshop launches.
 */
public class CacheRuntimeMarkerTest {

    @TempDir
    private Path directory;

    /**
     * Verifies cache marker creation and cleanup without relying on a Workshop code source.
     *
     * @throws Exception if fixture directories cannot be created
     */
    @Test
    public void shouldPublishMarkerInResolvedCacheRoot() throws Exception {
        Path game = Files.createDirectories(directory.resolve("game"));
        Path cache = directory.resolve("custom-cache");
        String originalDirectory = System.getProperty("user.dir");
        System.setProperty("user.dir", game.toString());
        Path marker = cache.resolve(ZomboidConstants.RUNTIME_MARKER_NAME);
        try (var runtime = new ZomboidRuntimeMarker()) {
            assertThat(runtime.activate(cache)).contains(marker, game.resolve(ZomboidConstants.RUNTIME_MARKER_NAME));
            assertThat(Long.parseLong(Files.readString(marker))).isPositive();
            assertThat(runtime.activate(cache)).doesNotHaveDuplicates();
        } finally {
            System.setProperty("user.dir", originalDirectory);
        }
        assertThat(marker).doesNotExist();
        assertThat(game.resolve(ZomboidConstants.RUNTIME_MARKER_NAME)).doesNotExist();
    }
}
