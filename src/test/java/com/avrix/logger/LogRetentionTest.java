package com.avrix.logger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for cross-launch log retention.
 */
class LogRetentionTest {

    @TempDir
    private Path logDirectory;

    @Test
    void shouldDeleteExpiredLogFilesAndKeepRecentOnes() throws Exception {
        Path expired = writeLog("avrix-expired.log", 16, Instant.now().minus(10, ChronoUnit.DAYS));
        Path legacyExpired = writeLog("avrix.log", 16, Instant.now().minus(10, ChronoUnit.DAYS));
        Path recent = writeLog("avrix-recent.log", 16, Instant.now().minus(1, ChronoUnit.DAYS));
        Path unrelated = writeLog("other.log", 16, Instant.now().minus(10, ChronoUnit.DAYS));

        LogRetention.cleanup(logDirectory, 7, 1_000L);

        assertThat(expired).doesNotExist();
        assertThat(legacyExpired).doesNotExist();
        assertThat(recent).exists();
        assertThat(unrelated).exists();
    }

    @Test
    void shouldDeleteOldestFilesUntilTotalSizeCapIsMet() throws Exception {
        Path oldest = writeLog("avrix-oldest.log", 60, Instant.now().minus(3, ChronoUnit.DAYS));
        Path middle = writeLog("avrix-middle.log", 60, Instant.now().minus(2, ChronoUnit.DAYS));
        Path newest = writeLog("avrix-newest.log", 60, Instant.now().minus(1, ChronoUnit.DAYS));

        LogRetention.cleanup(logDirectory, 7, 100L);

        assertThat(oldest).doesNotExist();
        assertThat(middle).doesNotExist();
        assertThat(newest).exists();
    }

    @Test
    void shouldKeepNewestFileWhenItAloneExceedsSizeCap() throws Exception {
        Path only = writeLog("avrix-only.log", 200, Instant.now());

        LogRetention.cleanup(logDirectory, 7, 100L);

        assertThat(only).exists();
    }

    @Test
    void shouldIgnoreMissingLogDirectory() {
        LogRetention.cleanup(logDirectory.resolve("missing"), 7, 100L);
    }

    private Path writeLog(String name, int size, Instant modified) throws IOException {
        Path file = logDirectory.resolve(name);
        Files.writeString(file, "x".repeat(size));
        Files.setLastModifiedTime(file, FileTime.from(modified));
        return file;
    }
}
