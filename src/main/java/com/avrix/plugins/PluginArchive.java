package com.avrix.plugins;

import java.io.File;
import java.util.Objects;

/**
 * Discovered plugin archive together with its Project Zomboid mod source.
 *
 * @param file       plugin archive
 * @param modId      source mod identifier, or {@code null} for the plugins directory
 * @param modName    source mod display name, or {@code null}
 * @param workshopId Steam Workshop item identifier, or {@code null} for local mods and the plugins directory
 * @param trusted    {@code true} when the archive comes from the trusted plugins directory
 */
public record PluginArchive(File file, String modId, String modName, String workshopId, boolean trusted) {

    /**
     * Creates a validated plugin archive descriptor.
     *
     * @throws NullPointerException if {@code file} is null
     */
    public PluginArchive {
        Objects.requireNonNull(file, "Plugin archive file cannot be null");
    }
}
