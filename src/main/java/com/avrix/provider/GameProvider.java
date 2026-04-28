package com.avrix.provider;

import com.avrix.core.BaseClassLoader;
import com.avrix.core.Environment;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Contract for game-specific integration within the Avrix loader.
 * Defines lifecycle hooks, metadata, and environment configuration.
 */
public interface GameProvider {
    /**
     * Prepares the provider with an isolated classloader.
     * Invoked once before {@link #launch(String[])}.
     *
     * @param classLoader the loader for game and extension classes
     */
    void initialize(BaseClassLoader classLoader);

    /**
     * Starts the game or runtime with the final merged arguments.
     *
     * @param args resolved command-line arguments including provider defaults and user overrides
     */
    void launch(String[] args);

    /**
     * Provides the default command-line arguments required by this provider.
     *
     * @return an array of default arguments, may be empty
     */
    String[] getLaunchArgs();

    /**
     * Returns the human-readable display name of the game or provider.
     *
     * @return the display name
     */
    String getName();

    /**
     * Returns the unique human-readable identifier used for internal routing.
     *
     * @return the identifier (lowercase, no spaces)
     */
    String getId();

    /**
     * Returns the list of authors or maintainers responsible for this provider.
     *
     * @return list of author names
     */
    List<String> getAuthors();

    /**
     * Returns the license identifier under which the provider is distributed.
     *
     * @return SPDX identifier or short license name
     */
    String getLicense();

    /**
     * Returns the list of contact references for support or source access.
     *
     * @return list of URLs, emails, or repository links
     */
    List<String> getContacts();

    /**
     * Returns the normalized version string formatted for dependency resolution.
     *
     * @return semver-compatible version
     */
    String getNormalizedVersion();

    /**
     * Returns the original version string exactly as defined by the distribution.
     *
     * @return raw version string
     */
    String getRawVersion();

    /**
     * Returns the base working directory used for assets, configurations, and logs.
     *
     * @return the launch directory path
     */
    Path getLaunchDirectory();

    /**
     * Returns the fully qualified class name of the main entry point to invoke.
     *
     * @return entrypoint class name
     */
    String getEntrypoint();

    /**
     * Returns the target execution environment for this provider instance.
     *
     * @return environment type (e.g., {@code CLIENT}, {@code SERVER})
     */
    Environment getEnvironment();

    /**
     * Provides additional directories or JAR files to append to the runtime classpath.
     *
     * @return list of paths to Java libraries
     * @implSpec Default returns an immutable empty list.
     */
    default List<Path> getJavaLibsPath() {
        return List.of();
    }

    /**
     * Provides directories containing platform-specific native libraries.
     *
     * @return list of paths to native library directories
     * @implSpec Default returns an immutable empty list.
     */
    default List<Path> getNativeLibsPath() {
        return List.of();
    }

    /**
     * Provides internal key-value configuration arguments injected before launch.
     *
     * @return map of provider-specific arguments
     * @implSpec Default returns an immutable empty map.
     */
    default Map<String, String> getProviderArgs() {
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