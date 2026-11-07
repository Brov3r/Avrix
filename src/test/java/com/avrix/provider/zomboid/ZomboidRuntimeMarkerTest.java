package com.avrix.provider.zomboid;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ZomboidRuntimeMarker} file handling.
 */
@DisplayName("ZomboidRuntimeMarker Unit Tests")
class ZomboidRuntimeMarkerTest {

    @TempDir
    Path tempDir;

    private static final Method WRITE_MARKER = findMethod("writeMarker", Path.class);
    private static final Method REFRESH_MARKERS = findMethod("refreshMarkers", List.class);

    @Test
    @DisplayName("Should write a timestamp and refresh it in place")
    void shouldWriteAndRefreshMarker() throws Exception {
        Path marker = tempDir.resolve("avrix-active.marker");

        assertThat(writeMarker(marker)).isTrue();
        long first = readTimestamp(marker);

        Thread.sleep(20);
        refreshMarkers(List.of(marker));
        long second = readTimestamp(marker);

        assertThat(first).isPositive();
        assertThat(second).isGreaterThanOrEqualTo(first);
    }

    @Test
    @DisplayName("Should create and remove the working-directory marker")
    void shouldCreateAndRemoveWorkingDirectoryMarker() {
        String originalUserDir = System.getProperty("user.dir");
        System.setProperty("user.dir", tempDir.toString());

        try {
            ZomboidRuntimeMarker marker = new ZomboidRuntimeMarker();
            marker.activate();

            Path markerFile = tempDir.resolve(ZomboidConstants.RUNTIME_MARKER_NAME);
            assertThat(markerFile).isRegularFile();

            marker.deactivate();
            assertThat(markerFile).doesNotExist();
        } finally {
            System.setProperty("user.dir", originalUserDir);
        }
    }

    private static long readTimestamp(Path marker) throws Exception {
        return Long.parseLong(Files.readString(marker));
    }

    private static boolean writeMarker(Path marker) throws Exception {
        return (boolean) WRITE_MARKER.invoke(null, marker);
    }

    private static void refreshMarkers(List<Path> markers) throws Exception {
        REFRESH_MARKERS.invoke(null, markers);
    }

    private static Method findMethod(String name, Class<?>... parameterTypes) {
        try {
            Method method = ZomboidRuntimeMarker.class.getDeclaredMethod(name, parameterTypes);
            method.setAccessible(true);
            return method;
        } catch (NoSuchMethodException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }
}
