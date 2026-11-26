package com.avrix.plugins;

import java.util.List;
import java.util.Objects;

/**
 * Describes a class resource provided by more than one plugin archive.
 * <p>
 * Avrix uses a first-wins classpath, so a conflicting resource is never merged: the earliest attached archive wins and
 * later providers are ignored for that path. This record reports such overlaps without changing the classpath.
 *
 * @param resourcePath   class resource path inside the archive, such as {@code "com/example/Shared.class"}
 * @param pluginIds      identifiers of every plugin providing the resource, in discovery order; never empty
 * @param winnerPluginId identifier of the plugin whose copy wins through classpath order
 */
public record ClasspathConflict(String resourcePath, List<String> pluginIds, String winnerPluginId) {

    /**
     * Validates and defensively copies the conflict description.
     *
     * @throws NullPointerException     if {@code resourcePath}, {@code pluginIds}, any identifier, or
     *                                  {@code winnerPluginId} is {@code null}
     * @throws IllegalArgumentException if {@code pluginIds} is empty or {@code winnerPluginId} is not listed among
     *                                  its entries
     */
    public ClasspathConflict {
        Objects.requireNonNull(resourcePath, "Resource path cannot be null");
        Objects.requireNonNull(pluginIds, "Plugin ID list cannot be null");
        Objects.requireNonNull(winnerPluginId, "Winner plugin ID cannot be null");
        pluginIds = List.copyOf(pluginIds);
        if (pluginIds.isEmpty()) {
            throw new IllegalArgumentException("Conflict must list at least one plugin");
        }
        if (!pluginIds.contains(winnerPluginId)) {
            throw new IllegalArgumentException(
                    "Winner plugin [%s] must be listed among the conflicting plugins".formatted(winnerPluginId)
            );
        }
    }
}
