package com.avrix.provider;

import com.avrix.core.KnotClassLoader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.PrintStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Lifecycle tests for process-wide state managed by {@link ZomboidGameProvider}.
 */
@DisplayName("ZomboidGameProvider Lifecycle Tests")
class ZomboidGameProviderLifecycleTest {

    @TempDir
    Path tempDir;

    private String originalUserDir;
    private String originalJavaLibraryPath;
    private String originalLwjglLibraryPath;
    private String originalGameDir;
    private String originalZomboidDir;
    private PrintStream originalOut;
    private PrintStream originalErr;

    @AfterEach
    void tearDown() {
        restoreProperty("user.dir", originalUserDir);
        restoreProperty("java.library.path", originalJavaLibraryPath);
        restoreProperty("org.lwjgl.librarypath", originalLwjglLibraryPath);
        restoreProperty("avrix.game.dir", originalGameDir);
        restoreProperty("zomboid.dir", originalZomboidDir);
        System.setOut(originalOut);
        System.setErr(originalErr);
    }

    @Test
    @DisplayName("Should not clear system properties when closed before initialization")
    void shouldNotClearPropertiesWhenClosedBeforeInitialization() {
        captureSystemState();

        new ZomboidGameProvider().close();

        assertThat(System.getProperty("user.dir")).isEqualTo(originalUserDir);
        assertThat(System.getProperty("java.library.path")).isEqualTo(originalJavaLibraryPath);
        assertThat(System.getProperty("org.lwjgl.librarypath")).isEqualTo(originalLwjglLibraryPath);
    }

    @Test
    @DisplayName("Should restore system properties after a successful initialization")
    void shouldRestorePropertiesAfterSuccessfulInitialization() throws Exception {
        captureSystemState();
        Path gameDirectory = createClientDirectory();
        System.setProperty("avrix.game.dir", gameDirectory.toString());

        try (KnotClassLoader classLoader = new KnotClassLoader(new URL[0], getClass().getClassLoader())) {
            ZomboidGameProvider provider = new ZomboidGameProvider();
            provider.init(classLoader);

            assertThat(System.getProperty("user.dir"))
                    .isEqualTo(gameDirectory.toAbsolutePath().normalize().toString());

            provider.close();
        }

        assertThat(System.getProperty("user.dir")).isEqualTo(originalUserDir);
        assertThat(System.getProperty("java.library.path")).isEqualTo(originalJavaLibraryPath);
        assertThat(System.getProperty("org.lwjgl.librarypath")).isEqualTo(originalLwjglLibraryPath);
    }

    @Test
    @DisplayName("Should roll back captured state when initialization fails")
    void shouldRollBackStateWhenInitializationFails() throws Exception {
        captureSystemState();
        Path gameDirectory = createClientDirectory();
        System.setProperty("avrix.game.dir", gameDirectory.toString());

        try (KnotClassLoader classLoader = new KnotClassLoader(new URL[0], getClass().getClassLoader())) {
            ZomboidGameProvider provider = new ZomboidGameProvider() {
                /** {@inheritDoc} */
                @Override
                public List<Path> getJavaLibsPath() {
                    throw new IllegalStateException("simulated library failure");
                }
            };

            assertThatThrownBy(() -> provider.init(classLoader))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("simulated library failure");
        }

        assertThat(System.getProperty("user.dir")).isEqualTo(originalUserDir);
        assertThat(System.getProperty("java.library.path")).isEqualTo(originalJavaLibraryPath);
        assertThat(System.getProperty("org.lwjgl.librarypath")).isEqualTo(originalLwjglLibraryPath);
    }

    @Test
    @DisplayName("Should fall back to zomboid.dir when avrix.game.dir is blank")
    void shouldFallBackToAlternativeDirectoryProperty() throws Exception {
        captureSystemState();
        Path gameDirectory = createClientDirectory();
        System.setProperty("avrix.game.dir", " ");
        System.setProperty("zomboid.dir", gameDirectory.toString());

        ZomboidGameProvider provider = new ZomboidGameProvider();

        assertThat(provider.getLaunchDirectory())
                .isEqualTo(gameDirectory.toAbsolutePath().normalize());
    }

    @Test
    @DisplayName("Should restore standard streams after stream redirection is closed")
    void shouldRestoreStandardStreamsOnClose() throws Exception {
        captureSystemState();
        Path gameDirectory = createClientDirectory();
        System.setProperty("avrix.game.dir", gameDirectory.toString());

        try (KnotClassLoader classLoader = new KnotClassLoader(new URL[0], getClass().getClassLoader())) {
            ZomboidGameProvider provider = new ZomboidGameProvider();
            provider.init(classLoader);
            provider.redirectSystemStreamsToLogger();

            assertThat(System.out).isNotSameAs(originalOut);
            assertThat(System.err).isNotSameAs(originalErr);

            provider.close();
        }

        assertThat(System.out).isSameAs(originalOut);
        assertThat(System.err).isSameAs(originalErr);
    }

    private Path createClientDirectory() throws Exception {
        Path gameDirectory = Files.createDirectories(tempDir.resolve("game"));
        Files.createFile(gameDirectory.resolve("ProjectZomboid64.exe"));
        return gameDirectory;
    }

    private void captureSystemState() {
        originalUserDir = System.getProperty("user.dir");
        originalJavaLibraryPath = System.getProperty("java.library.path");
        originalLwjglLibraryPath = System.getProperty("org.lwjgl.librarypath");
        originalGameDir = System.getProperty("avrix.game.dir");
        originalZomboidDir = System.getProperty("zomboid.dir");
        originalOut = System.out;
        originalErr = System.err;
    }

    private static void restoreProperty(String key, String value) {
        if (value == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, value);
        }
    }
}
