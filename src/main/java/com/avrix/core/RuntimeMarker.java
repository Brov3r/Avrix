package com.avrix.core;

import com.avrix.utils.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Creates and removes runtime marker files that let Project Zomboid Lua code detect an active Avrix launch.
 * <p>
 * The marker is written to the current working directory and, when the loader runs from a Workshop mod
 * {@code libs} directory, to the adjacent {@code 42} directory that Project Zomboid exposes to Lua.
 */
public final class RuntimeMarker {

    private static final Logger LOGGER = LoggerFactory.getLogger(RuntimeMarker.class);

    /**
     * Prevents instantiation of this utility class.
     */
    private RuntimeMarker() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated.");
    }

    /**
     * Creates marker files for the current Avrix run.
     *
     * @return marker paths that were successfully created
     */
    public static List<Path> activate() {
        try {
            String content = Long.toString(System.currentTimeMillis());
            List<Path> markers = new ArrayList<>(2);

            Path workingDirectoryMarker = Path.of(System.getProperty("user.dir", "."))
                    .toAbsolutePath()
                    .normalize()
                    .resolve(Constants.RUNTIME_MARKER_NAME);
            writeMarker(workingDirectoryMarker, content, markers);

            Path modMarker = resolveModMarker();
            if (modMarker != null) {
                writeMarker(modMarker, content, markers);
            }

            return List.copyOf(markers);
        } catch (RuntimeException e) {
            LOGGER.debug("Failed to create Avrix runtime markers", e);
            return List.of();
        }
    }

    /**
     * Removes marker files created by {@link #activate()}.
     *
     * @param markers marker paths to remove
     */
    public static void deactivate(List<Path> markers) {
        if (markers == null) {
            return;
        }

        for (Path marker : markers) {
            try {
                Files.deleteIfExists(marker);
            } catch (IOException e) {
                LOGGER.debug("Failed to remove Avrix runtime marker [{}]", marker, e);
            }
        }
    }

    private static void writeMarker(Path marker, String content, List<Path> markers) {
        try {
            Files.createDirectories(marker.getParent());
            Files.writeString(
                    marker,
                    content,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE
            );
            markers.add(marker);
        } catch (IOException e) {
            LOGGER.debug("Failed to write Avrix runtime marker [{}]", marker, e);
        }
    }

    private static Path resolveModMarker() {
        try {
            CodeSource codeSource = RuntimeMarker.class.getProtectionDomain().getCodeSource();
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

            return versionDirectory.resolve(Constants.RUNTIME_MARKER_NAME);
        } catch (URISyntaxException e) {
            LOGGER.debug("Failed to resolve Avrix runtime marker location", e);
            return null;
        }
    }
}
