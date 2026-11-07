package com.avrix.provider.zomboid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Creates and removes the Project Zomboid runtime marker that lets Lua code detect an active Avrix launch.
 * <p>
 * The marker is written to the Lua cache directory, which is the only location exposed to Lua through
 * {@code getFileReader}. It is the single runtime marker used by the loader. On Windows the file is opened with
 * {@link StandardOpenOption#DELETE_ON_CLOSE}, so the operating system removes it even when the loader process is
 * killed and Lua only needs to check that the file exists. On other systems the marker stays readable and its
 * timestamp is refreshed periodically, so Lua can distinguish a live session from a marker left behind by a crash.
 */
public final class ZomboidRuntimeMarker implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZomboidRuntimeMarker.class);

    /**
     * Seconds between marker timestamp refreshes.
     */
    private static final long HEARTBEAT_INTERVAL_SECONDS = 30L;

    /**
     * Seconds to wait for the heartbeat task to stop during shutdown.
     */
    private static final long HEARTBEAT_SHUTDOWN_TIMEOUT_SECONDS = 2L;

    /**
     * Whether the platform keeps a file opened with {@link StandardOpenOption#DELETE_ON_CLOSE} visible.
     */
    private static final boolean DELETE_ON_CLOSE_VISIBLE = System.getProperty("os.name", "")
            .toLowerCase(Locale.ROOT)
            .startsWith("win");

    private Path marker;
    private FileChannel channel;
    private ScheduledExecutorService heartbeat;

    /**
     * Creates an inactive runtime marker manager.
     */
    public ZomboidRuntimeMarker() {
    }

    /**
     * Creates the marker in the current working directory.
     *
     * @return marker paths that were successfully created
     */
    public synchronized List<Path> activate() {
        return activate(null);
    }

    /**
     * Creates the runtime marker in the supplied game cache directory.
     * <p>
     * The marker is placed in the {@code Lua} subdirectory because Project Zomboid exposes only that directory to
     * {@code getFileReader}.
     *
     * @param cacheRoot resolved game cache directory, or {@code null} to use the current working directory
     * @return marker paths that were successfully created
     */
    public synchronized List<Path> activate(Path cacheRoot) {
        if (marker != null) {
            return List.of(marker);
        }

        Path directory = cacheRoot == null
                ? Path.of(System.getProperty("user.dir", ".")).toAbsolutePath().normalize()
                : cacheRoot.toAbsolutePath().normalize().resolve("Lua");
        Path target = directory.resolve(ZomboidConstants.RUNTIME_MARKER_NAME);

        FileChannel opened = null;
        try {
            Files.createDirectories(directory);
            if (DELETE_ON_CLOSE_VISIBLE) {
                opened = FileChannel.open(
                        target,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.WRITE,
                        StandardOpenOption.TRUNCATE_EXISTING,
                        StandardOpenOption.DELETE_ON_CLOSE
                );
                writeTimestamp(opened);
                channel = opened;
            } else {
                writeTimestamp(target);
            }
            marker = target;
            if (!DELETE_ON_CLOSE_VISIBLE) {
                startHeartbeat();
            }
            return List.of(target);
        } catch (IOException | UnsupportedOperationException e) {
            marker = null;
            channel = null;
            closeQuietly(opened);
            LOGGER.debug("Failed to create Avrix runtime marker [{}]", target, e);
            return List.of();
        }
    }

    /**
     * Stops the timestamp refresh and removes the marker.
     */
    public synchronized void deactivate() {
        stopHeartbeat();

        FileChannel currentChannel = channel;
        Path currentMarker = marker;
        channel = null;
        marker = null;

        closeQuietly(currentChannel);
        if (!DELETE_ON_CLOSE_VISIBLE && currentMarker != null) {
            try {
                Files.deleteIfExists(currentMarker);
            } catch (IOException e) {
                LOGGER.debug("Failed to remove Avrix runtime marker [{}]", currentMarker, e);
            }
        }
    }

    /**
     * Stops the timestamp refresh and removes the marker.
     */
    @Override
    public void close() {
        deactivate();
    }

    private void startHeartbeat() {
        Path watchedMarker = marker;
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "avrix-runtime-marker-heartbeat");
            thread.setDaemon(true);
            return thread;
        });

        executor.scheduleAtFixedRate(
                () -> refreshMarker(watchedMarker),
                HEARTBEAT_INTERVAL_SECONDS,
                HEARTBEAT_INTERVAL_SECONDS,
                TimeUnit.SECONDS
        );

        heartbeat = executor;
    }

    private void stopHeartbeat() {
        ScheduledExecutorService executor = heartbeat;
        heartbeat = null;

        if (executor == null) {
            return;
        }

        executor.shutdownNow();
        try {
            executor.awaitTermination(HEARTBEAT_SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void refreshMarker(Path marker) {
        try {
            writeTimestamp(marker);
        } catch (IOException e) {
            LOGGER.debug("Failed to refresh Avrix runtime marker [{}]", marker, e);
        }
    }

    private static void writeTimestamp(Path target) throws IOException {
        Files.writeString(
                target,
                timestampValue(),
                StandardCharsets.US_ASCII,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE
        );
    }

    private static void writeTimestamp(FileChannel target) throws IOException {
        ByteBuffer buffer = ByteBuffer.wrap(timestampValue().getBytes(StandardCharsets.US_ASCII));
        target.position(0);
        while (buffer.hasRemaining()) {
            target.write(buffer);
        }
        target.force(false);
    }

    private static String timestampValue() {
        return String.format(Locale.ROOT, "%020d", System.currentTimeMillis());
    }

    private static void closeQuietly(FileChannel target) {
        if (target == null) {
            return;
        }
        try {
            target.close();
        } catch (IOException ignored) {
            // The marker is best-effort; a close failure must not break game startup.
        }
    }
}
