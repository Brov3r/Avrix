package com.avrix.core;

import com.avrix.utils.Constants;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;

/**
 * Immutable configuration for the loader runtime.
 * <p>
 * Groups the loader-level paths, manifest names, target environment, and stream redirection flag so they can be
 * supplied without relying on static {@link Constants} values.
 *
 * @param pluginsDirectory       directory scanned for plugin archives; the loader factory resolves relative paths
 *                               against the game launch directory
 * @param metadataFileName       manifest file name inside each plugin archive
 * @param pluginExtension        file extension used to discover plugin archives
 * @param pluginIconName         icon file name inside each plugin archive
 * @param providerId             identifier of the game provider to load
 * @param environment            target environment used for plugin filtering, or {@link Environment#AUTO}
 *                               to detect it through the active game provider
 * @param redirectSystemStreams  whether game output streams should be redirected to the loader logger
 * @param strict                 whether plugin load failures should abort the entire startup
 */
public record LoaderConfig(
        Path pluginsDirectory,
        String metadataFileName,
        String pluginExtension,
        String pluginIconName,
        String providerId,
        Environment environment,
        boolean redirectSystemStreams,
        boolean strict
) {

    /**
     * Creates a validated loader configuration.
     *
     * @throws NullPointerException     if {@code pluginsDirectory}, {@code metadataFileName}, or {@code pluginIconName} is null
     * @throws IllegalArgumentException if a string value is blank
     */
    public LoaderConfig {
        Objects.requireNonNull(pluginsDirectory, "Plugins directory cannot be null");
        Objects.requireNonNull(metadataFileName, "Metadata file name cannot be null");
        Objects.requireNonNull(pluginExtension, "Plugin extension cannot be null");
        Objects.requireNonNull(pluginIconName, "Plugin icon name cannot be null");
        Objects.requireNonNull(providerId, "Game provider ID cannot be null");

        if (metadataFileName.isBlank()) {
            throw new IllegalArgumentException("Metadata file name cannot be blank");
        }
        if (pluginExtension.isBlank()) {
            throw new IllegalArgumentException("Plugin extension cannot be blank");
        }
        if (pluginIconName.isBlank()) {
            throw new IllegalArgumentException("Plugin icon name cannot be blank");
        }
        if (providerId.isBlank()) {
            throw new IllegalArgumentException("Game provider ID cannot be blank");
        }

        environment = Objects.requireNonNullElse(environment, Environment.BOTH);
        pluginExtension = pluginExtension.toLowerCase(Locale.ROOT);
        if (!pluginExtension.startsWith(".")) {
            pluginExtension = "." + pluginExtension;
        }
    }

    /**
     * Returns the default loader configuration.
     *
     * @return configuration using the standard plugins directory, manifest names, and stream redirection
     */
    public static LoaderConfig defaults() {
        return new LoaderConfig(
                Path.of(Constants.PLUGINS_FOLDER_NAME),
                Constants.METADATA_NAME,
                Constants.PLUGIN_EXTENSION,
                Constants.PLUGIN_ICON_NAME,
                Constants.AUTO_PROVIDER_ID,
                Environment.AUTO,
                true,
                false
        );
    }

    /**
     * Returns a copy of this configuration with a different plugins directory.
     *
     * @param pluginsDirectory directory scanned for plugin archives
     * @return configuration with the provided plugins directory
     * @throws NullPointerException if {@code pluginsDirectory} is null
     */
    public LoaderConfig withPluginsDirectory(Path pluginsDirectory) {
        return new LoaderConfig(
                pluginsDirectory,
                metadataFileName,
                pluginExtension,
                pluginIconName,
                providerId,
                environment,
                redirectSystemStreams,
                strict
        );
    }

    /**
     * Returns a copy of this configuration with a different target environment.
     *
     * @param environment target environment used for plugin filtering
     * @return configuration with the provided environment
     */
    public LoaderConfig withEnvironment(Environment environment) {
        return new LoaderConfig(
                pluginsDirectory,
                metadataFileName,
                pluginExtension,
                pluginIconName,
                providerId,
                environment,
                redirectSystemStreams,
                strict
        );
    }

    /**
     * Returns a copy of this configuration with a different strict-loading flag.
     *
     * @param strict whether plugin load failures should abort startup
     * @return configuration with the provided strict-loading flag
     */
    public LoaderConfig withStrict(boolean strict) {
        return new LoaderConfig(
                pluginsDirectory,
                metadataFileName,
                pluginExtension,
                pluginIconName,
                providerId,
                environment,
                redirectSystemStreams,
                strict
        );
    }
}
