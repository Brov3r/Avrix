package com.avrix.logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Applies age and size retention to per-launch Avrix log files.
 * <p>
 * Logback rotates files inside one launch, but a unique active file name per launch means its rolling policy cannot
 * see files created by earlier launches. This utility enforces the same limits across launches and deliberately avoids
 * SLF4J because it runs before the logger subsystem is used.
 */
public final class LogRetention {

    private static final String LOG_DIR_PROPERTY = "avrix.log.dir";
    private static final Path DEFAULT_LOG_DIR = Path.of("./logs");
    private static final String LOG_FILE_PREFIX = "avrix-";
    private static final String LEGACY_LOG_FILE_PREFIX = "avrix.";
    private static final String LOG_FILE_SUFFIX = ".log";
    private static final int DEFAULT_MAX_HISTORY_DAYS = 7;
    private static final long DEFAULT_TOTAL_SIZE_CAP_BYTES = 1L << 30;

    /**
     * Prevents instantiation of this utility class.
     *
     * @throws UnsupportedOperationException always
     */
    private LogRetention() {
        throw new UnsupportedOperationException("LogRetention utility class cannot be instantiated");
    }

    /**
     * Deletes expired per-launch log files and enforces the total size cap.
     * <p>
     * Missing or unreadable log directories are ignored, and individual deletion failures do not stop cleanup. The
     * newest log file is always preserved, even when it alone exceeds the size cap.
     */
    public static void cleanup() {
        try {
            cleanup(resolveLogDirectory(), DEFAULT_MAX_HISTORY_DAYS, DEFAULT_TOTAL_SIZE_CAP_BYTES);
        } catch (RuntimeException e) {
            // Retention is best-effort and must never block startup.
        }
    }

    /**
     * Applies retention to an explicit log directory.
     *
     * @param logDirectory      directory to scan
     * @param maxHistoryDays    maximum file age in days; non-positive disables age-based deletion
     * @param totalSizeCapBytes maximum combined size of all log files; non-positive disables size-based deletion
     */
    public static void cleanup(Path logDirectory, int maxHistoryDays, long totalSizeCapBytes) {
        if (logDirectory == null || !Files.isDirectory(logDirectory)) {
            return;
        }

        List<LogFile> logFiles = listLogFiles(logDirectory);
        if (logFiles.isEmpty()) {
            return;
        }

        if (maxHistoryDays > 0) {
            Instant cutoff = Instant.now().minus(maxHistoryDays, ChronoUnit.DAYS);
            logFiles.removeIf(file -> {
                if (file.lastModified().isBefore(cutoff)) {
                    deleteQuietly(file.path());
                    return true;
                }
                return false;
            });
        }

        if (totalSizeCapBytes <= 0L) {
            return;
        }

        logFiles.sort(Comparator.comparing(LogFile::lastModified));
        long totalSize = logFiles.stream().mapToLong(LogFile::size).sum();
        for (int index = 0; index < logFiles.size() - 1 && totalSize > totalSizeCapBytes; index++) {
            LogFile file = logFiles.get(index);
            if (deleteQuietly(file.path())) {
                totalSize -= file.size();
            }
        }
    }

    private static List<LogFile> listLogFiles(Path logDirectory) {
        List<LogFile> files = new ArrayList<>();
        try (var paths = Files.list(logDirectory)) {
            paths.filter(Files::isRegularFile)
                    .filter(LogRetention::isLogFile)
                    .forEach(path -> addLogFile(path, files));
        } catch (IOException e) {
            return List.of();
        }
        return files;
    }

    private static void addLogFile(Path path, List<LogFile> target) {
        try {
            target.add(new LogFile(
                    path,
                    Files.size(path),
                    Files.getLastModifiedTime(path).toInstant()
            ));
        } catch (IOException e) {
            // Ignore files that disappear or become unreadable while the directory is being scanned.
        }
    }

    private static boolean isLogFile(Path path) {
        String name = path.getFileName().toString();
        if (!name.endsWith(LOG_FILE_SUFFIX)) {
            return false;
        }
        // Include the shared avrix.log scheme used before logs became per-launch.
        return name.startsWith(LOG_FILE_PREFIX) || name.startsWith(LEGACY_LOG_FILE_PREFIX);
    }

    private static boolean deleteQuietly(Path path) {
        try {
            return Files.deleteIfExists(path);
        } catch (IOException e) {
            return false;
        }
    }

    private static Path resolveLogDirectory() {
        String configured = System.getProperty(LOG_DIR_PROPERTY);
        if (configured == null || configured.isBlank()) {
            return DEFAULT_LOG_DIR;
        }

        try {
            return Path.of(configured);
        } catch (InvalidPathException e) {
            return DEFAULT_LOG_DIR;
        }
    }

    private record LogFile(Path path, long size, Instant lastModified) {
    }
}
