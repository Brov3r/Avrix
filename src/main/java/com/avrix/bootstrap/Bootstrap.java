package com.avrix.bootstrap;

import com.avrix.enums.Environment;
import com.avrix.provider.GameProvider;

import java.net.URLClassLoader;

/**
 * Main interface for initializing and launching the game application.
 *
 * <p>Typical usage:
 * <pre>
 * Bootstrap bootstrap = new DefaultBootstrap();
 * bootstrap.initialize(gameProvider);
 * bootstrap.launch(args);
 * </pre>
 *
 * @see com.avrix.bootstrap.DefaultBootstrap
 */
public interface Bootstrap {

    /**
     * Initializes the bootstrap with the specified game provider.
     * Must be called before {@link #launch(String[])}.
     *
     * @param provider the game provider to initialize with
     * @throws IllegalStateException if already initialized
     */
    void initialize(GameProvider provider);

    /**
     * Launches the loader with the given command line arguments.
     *
     * @param args command line arguments
     * @throws IllegalStateException if not initialized
     */
    void launch(String[] args);

    /**
     * Returns the class loader used by this bootstrap.
     *
     * @return the URLClassLoader instance
     */
    URLClassLoader getClassLoader();

    /**
     * Returns the {@link GameProvider} associated with this bootstrap.
     *
     * @return the {@link GameProvider} instance
     */
    GameProvider getProvider();

    /**
     * Returns the display name of the loader.
     *
     * @return loader name
     */
    String getName();

    /**
     * Returns the unique identifier of the loader.
     *
     * @return loader ID
     */
    String getId();

    /**
     * Returns the author(s) of the loader.
     *
     * @return author information
     */
    String getAuthor();

    /**
     * Returns the license under which the loader is distributed.
     *
     * @return license information
     */
    String getLicense();

    /**
     * Returns the version of the loader.
     *
     * @return version string
     */
    String getVersion();

    /**
     * Returns contact information for the loader.
     *
     * @return contact details
     */
    String getContacts();

    /**
     * Returns the current {@link Environment} ({@link Environment#CLIENT} or {@link Environment#SERVER}).
     *
     * @return {@link Environment} type
     */
    Environment getEnvironment();
}