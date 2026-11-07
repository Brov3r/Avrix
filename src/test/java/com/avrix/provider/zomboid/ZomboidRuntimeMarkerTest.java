package com.avrix.provider.zomboid;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ZomboidRuntimeMarker} file handling.
 */
@DisplayName("ZomboidRuntimeMarker Unit Tests")
class ZomboidRuntimeMarkerTest {

    @TempDir
    Path tempDir;

    /**
     * Verifies that activation creates one marker and deactivation removes it.
     *
     * @throws Exception if the marker cannot be read
     */
    @Test
    @DisplayName("Should create and remove the working-directory marker")
    void shouldCreateAndRemoveWorkingDirectoryMarker() throws Exception {
        String originalUserDir = System.getProperty("user.dir");
        System.setProperty("user.dir", tempDir.toString());

        try {
            ZomboidRuntimeMarker marker = new ZomboidRuntimeMarker();
            Path markerFile = tempDir.resolve(ZomboidConstants.RUNTIME_MARKER_NAME);

            assertThat(marker.activate()).containsExactly(markerFile);
            if (System.getProperty("os.name", "").toLowerCase(Locale.ROOT).startsWith("win")) {
                assertThat(markerFile).isRegularFile();
            } else {
                assertThat(Long.parseLong(Files.readString(markerFile))).isPositive();
            }
            assertThat(marker.activate()).containsExactly(markerFile);

            marker.deactivate();
            assertThat(markerFile).doesNotExist();
        } finally {
            System.setProperty("user.dir", originalUserDir);
        }
    }
}
