package com.avrix.provider.zomboid.approval;

import java.nio.file.Path;
import java.util.List;
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
 * @param mixinClasses         mixin transformer class names declared in the plugin metadata
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
        String previousSha256,
        List<String> mixinClasses
) {

    /**
     * Creates a validated approval entry.
     * <p>
     * Human-readable names are truncated for presentation. Identifiers, hashes, paths, and mixin declarations are
     * never truncated; oversized values are rejected instead.
     *
     * @throws NullPointerException if {@code jarPath} or {@code sha256} is null
     * @throws IllegalArgumentException if identity fields, paths, or mixin declarations exceed approval limits
     */
    public ZomboidApprovalEntry {
        modId = Objects.requireNonNullElse(modId, "");
        pluginId = Objects.requireNonNullElse(pluginId, "");
        modAuthor = ApprovalLimits.displayText(Objects.requireNonNullElse(modAuthor, ""));
        displayName = ApprovalLimits.displayText(Objects.requireNonNullElse(displayName, ""));
        sourceType = Objects.requireNonNullElse(sourceType, "");
        sourceName = ApprovalLimits.displayText(Objects.requireNonNullElse(sourceName, ""));
        workshopId = Objects.requireNonNullElse(workshopId, "");
        jarPath = Objects.requireNonNull(jarPath, "JAR path cannot be null");
        sha256 = Objects.requireNonNull(sha256, "SHA-256 cannot be null");
        previousSha256 = Objects.requireNonNullElse(previousSha256, "");
        for (String identity : List.of(modId, pluginId, sourceType, workshopId, sha256, previousSha256)) {
            ApprovalLimits.requireLength(identity, ApprovalLimits.MAX_IDENTIFIER_CHARACTERS, "Identity field");
        }
        ApprovalLimits.requireLength(jarPath.toString(), ApprovalLimits.MAX_FIELD_CHARACTERS, "Archive path");
        if (mixinClasses != null && mixinClasses.size() > ApprovalLimits.MAX_MIXINS) {
            throw new IllegalArgumentException("Too many mixin declarations for approval");
        }
        mixinClasses = List.copyOf(mixinClasses == null ? List.of() : mixinClasses.stream()
                .filter(Objects::nonNull)
                .map(String::strip)
                .filter(value -> !value.isEmpty())
                .distinct()
                .toList());
        int mixinCharacters = 0;
        for (String mixin : mixinClasses) {
            ApprovalLimits.requireLength(mixin, ApprovalLimits.MAX_IDENTIFIER_CHARACTERS, "Mixin class name");
            mixinCharacters += mixin.length() + 1;
        }
        if (mixinCharacters > ApprovalLimits.MAX_FIELD_CHARACTERS) {
            throw new IllegalArgumentException("Mixin declarations exceed approval text limit");
        }
    }
}
