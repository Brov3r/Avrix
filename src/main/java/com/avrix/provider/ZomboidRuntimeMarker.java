package com.avrix.provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Creates and removes Project Zomboid runtime marker files that let Lua code detect an active Avrix launch.
 * <p>
 * The marker is written to the current working directory and, when the loader runs from a Workshop mod
 * {@code libs} directory, to the adjacent {@code 42} directory that Project Zomboid exposes to Lua. While the loader
 * is running the timestamp is refreshed periodically, so Lua code can distinguish a live session from a marker left
 * behind by a crash.
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

    private final List<Path> markers = new ArrayList<>();
    private ScheduledExecutorService heartbeat;

    /**
     * Creates an inactive runtime marker manager.
     */
    public ZomboidRuntimeMarker() {
    }

    /**
     * Creates marker files for the current Avrix run and starts refreshing their timestamps.
     *
     * @return marker paths that were successfully created
     */
    public synchronized List<Path> activate() {
        if (!markers.isEmpty()) {
            return List.copyOf(markers);
        }

        try {
            Path workingDirectoryMarker = Path.of(System.getProperty("user.dir", "."))
                    .toAbsolutePath()
                    .normalize()
                    .resolve(ZomboidConstants.RUNTIME_MARKER_NAME);
            addMarker(workingDirectoryMarker);

            Path modMarker = resolveModMarker();
            if (modMarker != null) {
                addMarker(modMarker);
            }

            if (markers.isEmpty()) {
                return List.of();
            }

            startHeartbeat();
            return List.copyOf(markers);
        } catch (RuntimeException e) {
            LOGGER.debug("Failed to create Avrix runtime markers", e);
            return List.of();
        }
    }

    /**
     * Stops the timestamp refresh and removes marker files created by {@link #activate()}.
     */
    public synchronized void deactivate() {
        stopHeartbeat();

        for (Path marker : markers) {
            try {
                Files.deleteIfExists(marker);
            } catch (IOException e) {
                LOGGER.debug("Failed to remove Avrix runtime marker [{}]", marker, e);
            }
        }
        markers.clear();
    }

    /**
     * Stops the timestamp refresh and removes all created marker files.
     */
    @Override
    public void close() {
        deactivate();
    }

    private void addMarker(Path marker) {
        if (writeMarker(marker)) {
            markers.add(marker);
        }
    }

    private void startHeartbeat() {
        if (heartbeat != null) {
            return;
        }

        List<Path> watchedMarkers = List.copyOf(markers);
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "avrix-runtime-marker-heartbeat");
            thread.setDaemon(true);
            return thread;
        });

        executor.scheduleAtFixedRate(
                () -> refreshMarkers(watchedMarkers),
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

    private static void refreshMarkers(List<Path> markers) {
        for (Path marker : markers) {
            writeMarker(marker);
        }
    }

    private static boolean writeMarker(Path marker) {
        try {
            Path parent = marker.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            Path temporary = marker.resolveSibling(marker.getFileName() + ".tmp");
            Files.writeString(
                    temporary,
                    Long.toString(System.currentTimeMillis()),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE
            );

            try {
                Files.move(
                        temporary,
                        marker,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE
                );
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporary, marker, StandardCopyOption.REPLACE_EXISTING);
            }

            return true;
        } catch (IOException e) {
            LOGGER.debug("Failed to write Avrix runtime marker [{}]", marker, e);
            return false;
        }
    }

    private static Path resolveModMarker() {
        try {
            CodeSource codeSource = ZomboidRuntimeMarker.class.getProtectionDomain().getCodeSource();
            if (codeSource == null || codeSource.getLocation() == null) {
                return null;
            }

            Path codePath = Paths.get(codeSource.getLocation().toURI()).toAbsolutePath().normalize();
            if (!Files.isRegularFile(codePath)) {
                return null;
            }

            Path libsDirectory = codePath.getParent();
            if (libsDirectory == null
                    || libsDirectory.getFileName() == null
                    || !"libs".equals(libsDirectory.getFileName().toString().toLowerCase(Locale.ROOT))) {
                return null;
            }

            Path modRoot = libsDirectory.getParent();
            if (modRoot == null) {
                return null;
            }

            Path versionDirectory = modRoot.resolve("42");
            if (!Files.isDirectory(versionDirectory)) {
                return null;
            }

            return versionDirectory.resolve(ZomboidConstants.RUNTIME_MARKER_NAME);
        } catch (URISyntaxException e) {
            LOGGER.debug("Failed to resolve Avrix runtime marker location", e);
            return null;
        }
    }
}
