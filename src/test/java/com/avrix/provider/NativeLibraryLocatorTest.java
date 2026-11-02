package com.avrix.provider;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link NativeLibraryLocator}.
 */
@DisplayName("NativeLibraryLocator Unit Tests")
class NativeLibraryLocatorTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("Should resolve Windows 64-bit client native directories")
    void shouldResolveWindowsNativeDirectories() throws Exception {
        Path win64 = Files.createDirectories(tempDir.resolve("win64"));
        Path nested = Files.createDirectories(tempDir.resolve("natives").resolve("win64"));

        List<Path> paths = new NativeLibraryLocator("Windows 11", "amd64").locate(tempDir, false);

        assertThat(paths)
                .contains(tempDir.toAbsolutePath().normalize(), win64, nested);
    }

    @Test
    @DisplayName("Should resolve Linux ARM64 native directories")
    void shouldResolveLinuxArm64NativeDirectories() throws Exception {
        Path arm64 = Files.createDirectories(tempDir.resolve("linux-arm64"));
        Path aarch64 = Files.createDirectories(tempDir.resolve("natives").resolve("linux-aarch64"));

        List<Path> paths = new NativeLibraryLocator("Linux", "aarch64").locate(tempDir, false);

        assertThat(paths).contains(arm64, aarch64);
    }

    @Test
    @DisplayName("Should resolve macOS ARM64 native directories")
    void shouldResolveMacArm64NativeDirectories() throws Exception {
        Path macArm64 = Files.createDirectories(tempDir.resolve("macarm64"));
        Path nested = Files.createDirectories(tempDir.resolve("natives").resolve("macarm64"));

        List<Path> paths = new NativeLibraryLocator("Mac OS X", "aarch64").locate(tempDir, false);

        assertThat(paths).contains(macArm64, nested);
    }

    @Test
    @DisplayName("Should include server native subdirectories")
    void shouldResolveServerNativeDirectories() throws Exception {
        Path natives = Files.createDirectories(tempDir.resolve("natives"));
        Path platform = Files.createDirectories(natives.resolve("win64"));

        List<Path> paths = new NativeLibraryLocator("Windows", "amd64").locate(tempDir, true);

        assertThat(paths).contains(natives, platform);
    }

    @Test
    @DisplayName("Should return an empty list for a missing launch directory")
    void shouldReturnEmptyForMissingDirectory() {
        List<Path> paths = new NativeLibraryLocator("Windows", "amd64")
                .locate(tempDir.resolve("missing"), false);

        assertThat(paths).isEmpty();
    }
}
