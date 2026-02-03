package com.avrix.provider;

import com.avrix.enums.Environment;
import com.avrix.loaders.CoreClassLoader;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * {@link GameProvider} implementation for Project Zomboid.
 *
 * <p>
 * This provider supports both client and dedicated server distributions.
 * </p>
 */
public interface GameProvider {

    /**
     * Initializes the provider with the given class loader.
     * <p>
     * This method should set up the necessary class loader and prepare any required resources, such as libraries or
     * native paths.
     * </p>
     *
     * @param classLoader the {@link CoreClassLoader} used for game execution; must not be {@code null}.
     */
    void initialize(CoreClassLoader classLoader);

    /**
     * Launches the game by invoking the entry point.
     * <p>
     * This method should be called after the provider has been initialized. It typically launches the game by
     * invoking the `main(String[])` method of the game entry point class.
     * </p>
     *
     * @param args command-line arguments to pass to the game entry point; may be {@code null}.
     */
    void launch(String[] args);

    /**
     * Returns the launch arguments passed to the game provider.
     * <p>
     * This method can be used to retrieve any arguments provided when launching the game.
     * </p>
     *
     * @return the launch arguments, never {@code null}.
     */
    String[] getLaunchArgs();

    /**
     * Returns the name of the game provider.
     *
     * @return the name of the game provider.
     */
    String getName();

    /**
     * Returns the unique identifier for the game provider.
     *
     * @return the identifier of the game provider.
     */
    String getId();

    /**
     * Returns the author of the game provider.
     *
     * @return the author of the game provider.
     */
    String getAuthor();

    /**
     * Returns the license type of the game provider.
     *
     * @return the license of the game provider.
     */
    String getLicense();

    /**
     * Returns the contacts of the game provider.
     *
     * @return the contacts of the game provider.
     */
    String getContacts();


    /**
     * Returns the normalized version string of the game.
     * <p>
     * This method returns the version in a standard format (e.g., major.minor.patch). If the version cannot
     * be determined, a default value will be returned.
     * </p>
     *
     * @return the normalized version of the game, never {@code null}.
     */
    String getNormalizedVersion();

    /**
     * Returns the raw version string of the game.
     * <p>
     * This method returns the version string as retrieved directly from the game runtime. It may include extra
     * metadata or an unprocessed version string.
     * </p>
     *
     * @return the raw version string of the game, or {@code null} if unavailable.
     */
    String getRawVersion();

    /**
     * Returns the launch directory for the game.
     * <p>
     * This method returns the directory where the game executable or core files are located.
     * </p>
     *
     * @return the path to the launch directory.
     */
    Path getLaunchDirectory();

    /**
     * Returns the entry point class name for the game.
     * <p>
     * This method returns the class name that contains the main method used to launch the game.
     * </p>
     *
     * @return the entry point class name.
     */
    String getEntrypoint();

    /**
     * Returns the environment type of the game.
     * <p>
     * This method returns whether the game is running in a server or client environment.
     * </p>
     *
     * @return the environment type (either {@link Environment#CLIENT} or {@link Environment#SERVER}).
     */
    Environment getEnvironment();

    /**
     * Returns a list of Java library paths required by the game.
     * <p>
     * This method returns a list of JAR file paths that are required to run the game. By default, it returns
     * an empty list.
     * </p>
     *
     * @return a list of paths to Java libraries, possibly empty.
     */
    default List<Path> getJavaLibs() {
        return List.of();
    }

    /**
     * Returns a list of native library search paths required by the game.
     * <p>
     * This method returns a list of directories that contain native libraries required by the game. By default,
     * it returns an empty list.
     * </p>
     *
     * @return a list of paths to native library directories, possibly empty.
     */
    default List<Path> getNativePaths() {
        return List.of();
    }

    /**
     * Returns the core arguments used by the game provider.
     * <p>
     * This method returns any additional system properties or arguments that are required for the game provider
     * to function correctly. By default, it returns an empty map.
     * </p>
     *
     * @return a map of core arguments, possibly empty.
     */
    default Map<String, String> getProviderCoreArgs() {
        return Map.of();
    }

    /**
     * Redirects system output streams (stdout and stderr) to a logger.
     * <p>
     * This method can be overridden to redirect the system output streams to a logging system. By default, this
     * method does nothing (no-op).
     * </p>
     */
    default void redirectSystemStreamsToLogger() {
        // no-op by default
    }
}