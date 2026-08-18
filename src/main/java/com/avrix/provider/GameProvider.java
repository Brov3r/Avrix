package com.avrix.provider;

import com.avrix.core.Environment;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Defines the contract for a game provider.
 * <p>
 * Implementations are responsible for discovering game resources, setting up the environment,
 * and launching the game executable or entrypoint.
 */
public interface GameProvider {

    /**
     * Initializes the game environment, class loaders, and required paths.
     */
    void init();

    /**
     * Launches the game with the specified arguments.
     *
     * @param args command-line arguments to pass to the game
     */
    void launch(String[] args);

    /**
     * Returns the arguments used to launch the game.
     *
     * @return an array of launch arguments
     */
    String[] getLaunchArgs();

    /**
     * Returns the display name of the game.
     *
     * @return the game name
     */
    String getName();

    /**
     * Returns the unique identifier of the provider.
     *
     * @return the provider ID
     */
    String getId();

    /**
     * Returns a list of the game's authors.
     *
     * @return a list of authors
     */
    List<String> getAuthors();

    /**
     * Returns the game's license.
     *
     * @return the license string
     */
    String getLicense();

    /**
     * Returns a list of contact URLs or information for the game.
     *
     * @return a list of contacts
     */
    List<String> getContacts();

    /**
     * Returns a normalized semantic version string of the game.
     *
     * @return the normalized version
     */
    String getNormalizedVersion();

    /**
     * Returns the raw version string of the game.
     *
     * @return the raw version
     */
    String getRawVersion();

    /**
     * Returns the base launch directory of the game.
     *
     * @return the path to the launch directory
     */
    Path getLaunchDirectory();

    /**
     * Returns the fully qualified name of the game's main class.
     *
     * @return the entrypoint class name
     */
    String getEntrypoint();

    /**
     * Returns the current execution environment (e.g., CLIENT or SERVER).
     *
     * @return the environment
     */
    Environment getEnvironment();

    /**
     * Returns paths to required Java libraries (JARs).
     *
     * @return a list of library paths
     */
    default List<Path> getJavaLibsPath() {
        return List.of();
    }

    /**
     * Returns paths to required native libraries.
     *
     * @return a list of native library paths
     */
    default List<Path> getNativeLibsPath() {
        return List.of();
    }

    /**
     * Returns a map of system properties required for the game to run.
     *
     * @return a map of system properties
     */
    default Map<String, String> getProviderArgs() {
        return Map.of();
    }

    /**
     * Redirects default output and error streams to the logger.
     */
    default void redirectSystemStreamsToLogger() {
    }
}