package com.avrix.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link RuntimeMarker} file handling.
 */
@DisplayName("RuntimeMarker Unit Tests")
class RuntimeMarkerTest {

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
    @DisplayName("Should remove every marker on deactivate")
    void shouldRemoveMarkersOnDeactivate() throws Exception {
        Path first = tempDir.resolve("first.marker");
        Path second = tempDir.resolve("second.marker");
        assertThat(writeMarker(first)).isTrue();
        assertThat(writeMarker(second)).isTrue();

        RuntimeMarker.deactivate(List.of(first, second));

        assertThat(first).doesNotExist();
        assertThat(second).doesNotExist();
    }

    @Test
    @DisplayName("Should accept a null marker list on deactivate")
    void shouldAcceptNullMarkers() {
        RuntimeMarker.deactivate(null);
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
            Method method = RuntimeMarker.class.getDeclaredMethod(name, parameterTypes);
            method.setAccessible(true);
            return method;
        } catch (NoSuchMethodException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }
}
