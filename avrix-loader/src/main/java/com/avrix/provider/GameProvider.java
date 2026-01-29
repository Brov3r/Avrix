package com.avrix.provider;

import com.avrix.enums.Environment;
import com.avrix.loaders.AvrixClassLoader;
import com.avrix.plugins.Metadata;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Service Provider Interface (SPI) representing a game runtime integration.
 */
public interface GameProvider {

    /**
     * Initializes the provider before game launch.
     *
     * @param classLoader the {@link AvrixClassLoader} used for game execution
     * @throws NullPointerException  if {@code classLoader} is {@code null}
     * @throws IllegalStateException if initialization fails or is invoked multiple times
     */
    void initialize(AvrixClassLoader classLoader);

    /**
     * Launches the game.
     *
     * <p>
     * Implementations are expected to transfer control to the game runtime.
     * This method may block until the game terminates or return immediately,
     * depending on the game architecture.
     * </p>
     *
     * @param args command-line arguments provided by the runtime
     * @throws IllegalStateException if the provider has not been initialized
     */
    void launch(String[] args);

    /**
     * Returns the stable identifier of this provider.
     *
     * @return provider identifier (recommended: lowercase ASCII, no spaces)
     */
    String getId();

    /**
     * Returns the human-readable game name.
     *
     * @return game name
     */
    String getGameName();

    /**
     * Returns the game or provider author.
     *
     * @return author or organization name
     */
    String getGameAuthor();

    /**
     * Returns the normalized game version.
     *
     * @return normalized version string
     */
    String getNormalizedVersion();

    /**
     * Returns the raw version string as reported by the game.
     *
     * <p>
     * This value is informational and may not be suitable for comparison.
     * </p>
     *
     * @return raw version string
     */
    String getRawVersion();

    /**
     * Returns the fully qualified name of the main entrypoint class.
     *
     * @return entrypoint class name
     */
    String getEntrypoint();

    /**
     * Returns the execution environment of the game.
     *
     * @return environment (e.g. CLIENT or SERVER)
     */
    Environment getEnvironment();

    /**
     * Returns additional Java classpath entries required by the game.
     *
     * <p>
     * Returned paths are expected to reference JAR files or directories.
     * The returned list must be immutable or a defensive copy.
     * </p>
     *
     * @return list of Java library paths, or an empty list if none are required
     */
    default List<Path> getJavaLibs() {
        return List.of();
    }

    /**
     * Returns directories where native libraries may be located.
     *
     * <p>
     * These directories are registered in the {@link AvrixClassLoader}
     * for explicit native library resolution.
     * </p>
     *
     * @return list of native library directories, or an empty list if none are required
     */
    default List<Path> getNativePaths() {
        return List.of();
    }

    /**
     * Returns structured metadata associated with this provider.
     *
     * @return metadata object, never {@code null}
     */
    Metadata getMetadata();

    /**
     * Returns arguments for the provider core/runtime.
     *
     * <p>
     * These arguments are intended for internal runtime configuration
     * and are not passed directly to the game process.
     * </p>
     *
     * @return immutable map of argument keys and values
     */
    default Map<String, String> getProviderCoreArgs() {
        return Map.of();
    }

    /**
     * Returns the directory from which the game should be launched.
     *
     * <p>
     * The returned path must exist and be accessible at launch time.
     * </p>
     *
     * @return launch working directory
     * @throws IllegalStateException if the launch directory cannot be resolved
     */
    Path launchDirectory();

    /**
     * Getting Application Launch Arguments
     *
     * @return application launch arguments passed from the Main entry point
     */
    String[] getLaunchArgs();

    /**
     * Redirects {@link System#out} and {@link System#err} to the logging system.
     *
     * <p>
     * Implementations may override this method to integrate game output
     * into the runtime logging infrastructure.
     * </p>
     */
    default void redirectSystemStreamsToLogger() {
        // no-op by default
    }

    /**
     * Indicates whether this provider represents a server environment.
     *
     * <p>
     * This is a convenience method derived from {@link #getEnvironment()}.
     * Prefer using {@link #getEnvironment()} for extensibility.
     * </p>
     *
     * @return {@code true} if the environment is {@link Environment#SERVER}
     */
    default boolean isServer() {
        return getEnvironment() == Environment.SERVER;
    }
}