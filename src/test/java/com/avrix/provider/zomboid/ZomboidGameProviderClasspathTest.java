package com.avrix.provider.zomboid;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for Project Zomboid classpath discovery.
 */
@DisplayName("ZomboidGameProvider Classpath Tests")
class ZomboidGameProviderClasspathTest {

    @TempDir
    Path tempDir;

    private String previousGameDir;

    @AfterEach
    void tearDown() {
        if (previousGameDir == null) {
            System.clearProperty("avrix.game.dir");
        } else {
            System.setProperty("avrix.game.dir", previousGameDir);
        }
    }

    @Test
    @DisplayName("Should list client JARs in deterministic name order")
    void shouldListClientJarsInNameOrder() throws Exception {
        Files.createFile(tempDir.resolve("ProjectZomboid64.exe"));
        Files.createFile(tempDir.resolve("z-library.jar"));
        Files.createFile(tempDir.resolve("a-library.jar"));
        Files.writeString(tempDir.resolve("readme.txt"), "ignored");

        useGameDirectory(tempDir);
        List<String> jars = new ZomboidGameProvider().getJavaLibsPath().stream()
                .map(path -> path.getFileName().toString())
                .toList();

        assertThat(jars).containsExactly("a-library.jar", "z-library.jar");
    }

    @Test
    @DisplayName("Should return an empty classpath when no JARs exist")
    void shouldReturnEmptyClasspath() throws Exception {
        Files.createFile(tempDir.resolve("ProjectZomboid64.exe"));
        useGameDirectory(tempDir);

        assertThat(new ZomboidGameProvider().getJavaLibsPath()).isEmpty();
    }

    private void useGameDirectory(Path directory) {
        previousGameDir = System.getProperty("avrix.game.dir");
        System.setProperty("avrix.game.dir", directory.toAbsolutePath().toString());
    }
}
