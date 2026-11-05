package com.avrix.provider.approval;

import java.nio.file.Path;
import java.util.Objects;

/**
 * One Java plugin archive that requires a user decision.
 *
 * @param modId                source mod identifier
 * @param pluginId             plugin identifier from the archive metadata
 * @param modAuthor            author declared by the source mod, or an empty string when unavailable
 * @param displayName          human-readable plugin name from the archive metadata
 * @param sourceType           source kind, such as {@code workshop} or {@code mod}
 * @param sourceName           human-readable source name
 * @param workshopId           Steam Workshop item identifier, or an empty string when unavailable
 * @param jarPath              absolute path to the plugin JAR
 * @param sha256               lowercase SHA-256 of the JAR
 * @param size                 JAR size in bytes
 * @param modifiedEpochMillis  last-modified time in epoch milliseconds, or {@code 0} when unavailable
 * @param previousSha256       previously approved hash for the same mod, or an empty string
 */
public record ZomboidApprovalEntry(
        String modId,
        String pluginId,
        String modAuthor,
        String displayName,
        String sourceType,
        String sourceName,
        String workshopId,
        Path jarPath,
        String sha256,
        long size,
        long modifiedEpochMillis,
        String previousSha256
) {

    /**
     * Creates a validated approval entry.
     *
     * @throws NullPointerException if {@code jarPath} or {@code sha256} is null
     */
    public ZomboidApprovalEntry {
        modId = Objects.requireNonNullElse(modId, "");
        pluginId = Objects.requireNonNullElse(pluginId, "");
        modAuthor = Objects.requireNonNullElse(modAuthor, "");
        displayName = Objects.requireNonNullElse(displayName, "");
        sourceType = Objects.requireNonNullElse(sourceType, "");
        sourceName = Objects.requireNonNullElse(sourceName, "");
        workshopId = Objects.requireNonNullElse(workshopId, "");
        jarPath = Objects.requireNonNull(jarPath, "JAR path cannot be null");
        sha256 = Objects.requireNonNull(sha256, "SHA-256 cannot be null");
        previousSha256 = Objects.requireNonNullElse(previousSha256, "");
    }
}
