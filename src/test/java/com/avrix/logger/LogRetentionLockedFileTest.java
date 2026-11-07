package com.avrix.logger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that Windows deletion failures do not remove files from size accounting.
 */
public class LogRetentionLockedFileTest {

    @TempDir
    private Path directory;

    /**
     * Verifies that a locked expired file still counts toward the total retention cap.
     *
     * @throws Exception if fixture logs cannot be created or inspected
     */
    @Test
    @EnabledOnOs(OS.WINDOWS)
    public void shouldCountExpiredFileWhenDeletionFails() throws Exception {
        Path locked = log("avrix-locked.log", 60, 10);
        Path middle = log("avrix-middle.log", 90, 2);
        Path newest = log("avrix-newest.log", 90, 1);

        try (var input = new FileInputStream(locked.toFile())) {
            LogRetention.cleanup(directory, 7, 200);

            assertThat(locked).isRegularFile();
            assertThat(middle).doesNotExist();
            assertThat(newest).isRegularFile();
            assertThat(Files.size(locked) + Files.size(newest)).isEqualTo(150L);
        }
    }

    private Path log(String name, int bytes, int ageDays) throws Exception {
        Path path = Files.writeString(directory.resolve(name), "x".repeat(bytes));
        Files.setLastModifiedTime(path, FileTime.from(Instant.now().minus(ageDays, ChronoUnit.DAYS)));
        return path;
    }
}
