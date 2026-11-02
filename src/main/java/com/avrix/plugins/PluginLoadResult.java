package com.avrix.plugins;

import java.util.Objects;
import java.util.Optional;

/**
 * Immutable result of a plugin load attempt.
 *
 * @param pluginId identifier of the plugin, or the archive name when metadata could not be parsed
 * @param status   outcome of the attempt
 * @param message  human-readable details
 * @param error    failure cause, or {@code null} when the plugin loaded or was skipped
 */
public record PluginLoadResult(String pluginId, PluginLoadStatus status, String message, Throwable error) {

    /**
     * Creates a validated load result.
     *
     * @throws NullPointerException if {@code pluginId} or {@code status} is null
     */
    public PluginLoadResult {
        Objects.requireNonNull(pluginId, "Plugin ID cannot be null");
        Objects.requireNonNull(status, "Plugin load status cannot be null");
        message = Objects.requireNonNullElse(message, "");
    }

    /**
     * Creates a successful load result.
     *
     * @param pluginId plugin identifier
     * @return loaded result
     */
    public static PluginLoadResult loaded(String pluginId) {
        return new PluginLoadResult(pluginId, PluginLoadStatus.LOADED, "Loaded", null);
    }

    /**
     * Creates a failed load result.
     *
     * @param pluginId plugin identifier
     * @param error    failure cause
     * @return failed result
     */
    public static PluginLoadResult failed(String pluginId, Throwable error) {
        Objects.requireNonNull(error, "Failure cause cannot be null");
        String message = error.getClass().getSimpleName()
                + (error.getMessage() != null ? ": " + error.getMessage() : "");
        return new PluginLoadResult(pluginId, PluginLoadStatus.FAILED, message, error);
    }

    /**
     * Creates a skipped load result.
     *
     * @param pluginId plugin identifier
     * @param reason   skip reason
     * @return skipped result
     */
    public static PluginLoadResult skipped(String pluginId, String reason) {
        return new PluginLoadResult(pluginId, PluginLoadStatus.SKIPPED, reason, null);
    }

    /**
     * Returns the failure cause when the plugin failed.
     *
     * @return optional failure cause
     */
    public Optional<Throwable> errorCause() {
        return Optional.ofNullable(error);
    }
}
