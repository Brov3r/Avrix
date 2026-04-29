package com.avrix.core;

import com.avrix.plugins.PluginManager;
import com.avrix.provider.GameProvider;

import java.util.List;

/**
 * Core lifecycle manager and entry point.
 * Handles initialization, launch, and runtime component access.
 */
public interface Bootstrap {
    /**
     * Initializes the bootstrap context.
     * Must be called before {@link #launch(String[])}.
     */
    void init();

    /**
     * Starts the application/game with command-line arguments.
     *
     * @param args Arguments passed to the launched process.
     */
    void launch(String[] args);

    /**
     * @return List of project authors/contributors. Never {@code null}.
     */
    List<String> getAuthors();

    /**
     * @return Human-readable project name. Never {@code null}.
     */
    String getName();

    /**
     * @return Unique project identifier. Never {@code null}.
     */
    String getId();

    /**
     * @return License name or SPDX identifier (e.g. {@code "MIT"}). Never {@code null}.
     */
    String getLicense();

    /**
     * @return Contact information or URLs (e.g. email, website). Never {@code null}.
     */
    List<String> getContacts();

    /**
     * @return Current version string (e.g. {@code "1.0.0"}). Never {@code null}.
     */
    String getVersion();

    /**
     * @return The active {@link GameProvider} instance.
     */
    GameProvider getGameProvider();

    /**
     * @return The active {@link BaseClassLoader} instance.
     */
    BaseClassLoader getClassLoader();

    /**
     * @return The active {@link PluginManager} instance.
     */
    PluginManager getPluginManager();
}