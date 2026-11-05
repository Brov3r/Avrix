package com.avrix.api;

import com.avrix.core.Environment;
import com.avrix.core.KnotClassLoader;
import com.avrix.core.Metadata;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Defines the contract for a game provider.
 * <p>
 * Implementations are responsible for discovering game resources, setting up the environment,
 * interpreting game-specific launch arguments, supplying game-specific plugin sources, and launching the game.
 */
public interface GameProvider {

    /**
     * Initializes the game environment, class loaders, and required paths.
     */
    public void init();

    /**
     * Initializes the game environment with the loader classloader.
     *
     * @param classLoader flat classloader used to resolve game classes and attach libraries
     */
    public default void init(KnotClassLoader classLoader) {
        init();
    }

    /**
     * Applies game-specific command-line arguments before initialization.
     * <p>
     * Implementations may parse their own flags and store system properties required by the game. The loader
     * core does not interpret game-specific arguments.
     *
     * @param args command-line arguments passed to the game
     */
    public default void configure(String[] args) {
    }

    /**
     * Launches the game with the specified arguments.
     *
     * @param args command-line arguments to pass to the game
     */
    public void launch(String[] args);

    /**
     * Invoked immediately before {@link #launch(String[])}.
     * <p>
     * Providers can use this hook to create runtime artifacts such as marker files.
     */
    public default void beforeLaunch() {
    }

    /**
     * Invoked immediately after {@link #launch(String[])} returns or fails.
     * <p>
     * Providers can use this hook to remove runtime artifacts created by {@link #beforeLaunch()}.
     */
    public default void afterLaunch() {
    }

    /**
     * Returns the arguments used to launch the game.
     *
     * @return an array of launch arguments
     */
    public String[] getLaunchArgs();

    /**
     * Returns the display name of the game.
     *
     * @return the game name
     */
    public String getName();

    /**
     * Returns the unique identifier of the provider.
     *
     * @return the provider ID
     */
    public String getId();

    /**
     * Returns a list of the game's authors.
     *
     * @return a list of authors
     */
    public List<String> getAuthors();

    /**
     * Returns the game's license.
     *
     * @return the license string
     */
    public String getLicense();

    /**
     * Returns a list of contact URLs or information for the game.
     *
     * @return a list of contacts
     */
    public List<String> getContacts();

    /**
     * Returns a normalized semantic version string of the game.
     *
     * @return the normalized version
     */
    public String getNormalizedVersion();

    /**
     * Returns the raw version string of the game.
     *
     * @return the raw version
     */
    public String getRawVersion();

    /**
     * Returns the base launch directory of the game.
     *
     * @return the path to the launch directory
     */
    public Path getLaunchDirectory();

    /**
     * Returns the game user cache directory.
     * <p>
     * The default is a neutral {@code ~/.avrix} directory. Game providers should override this when the game
     * uses a different location for runtime hand-off files.
     *
     * @return cache directory path
     */
    public default Path getCacheRoot() {
        return Path.of(System.getProperty("user.home", ".")).resolve(".avrix");
    }

    /**
     * Returns the per-user directory used for loader configuration and persistent decisions.
     * <p>
     * The default is {@code ~/.avrix}. Providers should keep this outside the game directory so decisions survive
     * game updates and are not shared between operating-system users.
     *
     * @return configuration directory path
     */
    public default Path getConfigRoot() {
        return Path.of(System.getProperty("user.home", ".")).resolve(".avrix");
    }

    /**
     * Returns the fully qualified name of the game's main class.
     *
     * @return the entrypoint class name
     */
    public String getEntrypoint();

    /**
     * Returns the current execution environment (e.g., CLIENT or SERVER).
     *
     * @return the environment
     */
    public Environment getEnvironment();

    /**
     * Synthesizes metadata describing this game provider.
     * <p>
     * In the resulting descriptor, {@link Metadata#entrypoint()} denotes the game's main class rather than a
     * {@link com.avrix.plugins.Plugin} implementation.
     *
     * @return immutable metadata descriptor for the active game
     */
    public default Metadata getMetadata() {
        return new Metadata.Builder()
                .id(getId())
                .name(getName())
                .version(getNormalizedVersion())
                .environment(getEnvironment())
                .authors(getAuthors())
                .license(getLicense())
                .contacts(getContacts())
                .entrypoint(getEntrypoint())
                .build();
    }

    /**
     * Returns paths to required Java libraries (JARs).
     *
     * @return a list of library paths
     */
    public default List<Path> getJavaLibsPath() {
        return List.of();
    }

    /**
     * Returns paths to required native libraries.
     *
     * @return a list of native library paths
     */
    public default List<Path> getNativeLibsPath() {
        return List.of();
    }

    /**
     * Returns a map of system properties required for the game to run.
     *
     * @return a map of system properties
     */
    public default Map<String, String> getProviderArgs() {
        return Map.of();
    }

    /**
     * Returns additional plugin sources supplied by this game provider.
     *
     * @param strict whether discovery errors should abort startup
     * @return plugin sources, or an empty list when the provider has no additional sources
     */
    public default List<PluginSource> getPluginSources(boolean strict) {
        return List.of();
    }

    /**
     * Creates the approval gate used for archives from non-trusted plugin sources.
     *
     * @param cacheRoot   game cache directory
     * @param environment active runtime environment
     * @return approval gate, or an allow-all gate when the provider does not require approval
     */
    public default PluginApprovalGate createApprovalGate(Path cacheRoot, Environment environment) {
        return PluginApprovalGate.allowAll();
    }

    /**
     * Redirects default output and error streams to the logger.
     */
    public default void redirectSystemStreamsToLogger() {
    }

    /**
     * Releases provider resources and restores any process-wide state it changed.
     */
    public default void close() {
    }
}
