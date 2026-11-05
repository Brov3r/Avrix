package com.avrix.api;

import java.io.File;
import java.util.Map;
import java.util.Objects;

/**
 * Discovered plugin archive together with neutral source metadata.
 * <p>
 * Source-specific details such as a Workshop item identifier are carried in {@link #attributes()} so the loader
 * core does not depend on any particular game or distribution format.
 *
 * @param file       plugin archive
 * @param sourceId   identifier of the source package, or {@code null} for the plugins directory
 * @param sourceName display name of the source package, or {@code null}
 * @param sourceType source kind, such as {@code "plugins-directory"}, {@code "mod"}, or {@code "workshop"}
 * @param attributes immutable source-specific attributes
 * @param trusted    {@code true} when the archive comes from the manually managed plugins directory
 */
public record PluginArchive(
        File file,
        String sourceId,
        String sourceName,
        String sourceType,
        Map<String, String> attributes,
        boolean trusted
) {

    /**
     * Source type used for archives discovered in the manually managed plugins directory.
     */
    public static final String SOURCE_PLUGINS_DIRECTORY = "plugins-directory";

    /**
     * Source type used for archives discovered inside a game-specific mod package.
     */
    public static final String SOURCE_MOD = "mod";

    /**
     * Source type used for archives discovered inside a Workshop package.
     */
    public static final String SOURCE_WORKSHOP = "workshop";

    /**
     * Attribute key carrying a Workshop item identifier.
     */
    public static final String ATTRIBUTE_WORKSHOP_ID = "workshopId";

    /**
     * Attribute key carrying the source mod author, when declared by the game mod.
     */
    public static final String ATTRIBUTE_AUTHOR = "author";

    /**
     * Creates a validated plugin archive descriptor.
     *
     * @throws NullPointerException if {@code file} is null
     */
    public PluginArchive {
        Objects.requireNonNull(file, "Plugin archive file cannot be null");
        sourceType = (sourceType == null || sourceType.isBlank())
                ? "custom"
                : sourceType.strip();
        attributes = (attributes == null) ? Map.of() : Map.copyOf(attributes);
    }

    /**
     * Creates a trusted archive descriptor for a plugins-directory entry.
     *
     * @param file plugin archive
     * @return trusted archive descriptor
     */
    public static PluginArchive trusted(File file) {
        return new PluginArchive(file, null, null, SOURCE_PLUGINS_DIRECTORY, Map.of(), true);
    }
}
