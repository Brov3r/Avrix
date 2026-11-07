package com.avrix.provider.zomboid;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
        Path marker = cache.resolve("Lua").resolve(ZomboidConstants.RUNTIME_MARKER_NAME);
        boolean windows = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).startsWith("win");
        try (var runtime = new ZomboidRuntimeMarker()) {
            assertThat(runtime.activate(cache)).containsExactly(marker);
            assertThat(runtime.activate(cache)).containsExactly(marker);
            if (windows) {
                assertThat(marker).isRegularFile();
                assertThatThrownBy(() -> readWithFileInputStream(marker)).isInstanceOf(IOException.class);
            } else {
                assertThat(Long.parseLong(Files.readString(marker))).isPositive();
            }
        } finally {
            System.setProperty("user.dir", originalDirectory);
        }
        assertThat(marker).doesNotExist();
        assertThat(cache.resolve(ZomboidConstants.RUNTIME_MARKER_NAME)).doesNotExist();
        assertThat(game.resolve(ZomboidConstants.RUNTIME_MARKER_NAME)).doesNotExist();
    }

    private static void readWithFileInputStream(Path marker) throws IOException {
        try (FileInputStream input = new FileInputStream(marker.toFile())) {
            input.read();
        }
    }
}
