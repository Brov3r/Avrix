package com.avrix.core;

import com.avrix.mixin.MixinAgentBootstrap;
import com.avrix.plugins.PluginManager;
import com.avrix.provider.GameProvider;
import com.avrix.utils.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * The central bootstrap class responsible for initializing and launching the Avrix loader.
 */
public class AvrixBootstrap implements Bootstrap {
    private static final Logger log = LoggerFactory.getLogger(AvrixBootstrap.class);

    private final BaseClassLoader classLoader;
    private final PluginManager pluginManager;
    private final GameProvider provider;

    private static boolean isLaunched = false;
    private static boolean isInitialized = false;

    private static Bootstrap instance;

    /**
     * Initializes core components and registers this instance as the global singleton.
     *
     * @param classLoader   Class loader for dynamic resolution. Must not be {@code null}.
     * @param pluginManager Plugin management subsystem. Must not be {@code null}.
     * @param provider      Game/environment provider. Must not be {@code null}.
     * @throws NullPointerException if any parameter is {@code null}
     */
    public AvrixBootstrap(BaseClassLoader classLoader, PluginManager pluginManager, GameProvider provider) {
        this.classLoader = Objects.requireNonNull(classLoader, "ClassLoader can't be null!");
        this.pluginManager = Objects.requireNonNull(pluginManager, "PluginManager can't be null!");
        this.provider = Objects.requireNonNull(provider, "GameProvider can't be null!");

        instance = this;
    }

    /**
     * Retrieves the singleton Bootstrap instance.
     *
     * @return Active Bootstrap instance.
     * @throws IllegalStateException if the instance is missing.
     */
    public static Bootstrap getInstance() {
        if (instance == null) {
            throw new IllegalStateException("Bootstrap not created!");
        }
        return instance;
    }


    /**
     * Initializes the bootstrap context.
     * Must be called before {@link #launch(String[])}.
     */
    @Override
    public void init() {
        if (isInitialized) {
            log.warn("Bootstrap already initialized – skipping");
            return;
        }

        long startTime = System.currentTimeMillis();
        log.info("Bootstrap starting...");

        try {
            MixinAgentBootstrap.loadAgent(classLoader);

            provider.initialize(classLoader);

            pluginManager.init();
            pluginManager.loadPlugins();

            isInitialized = true;
            long duration = System.currentTimeMillis() - startTime;

            log.info("Bootstrap completed in {} ms", duration);
        } catch (Exception e) {
            isInitialized = false;
            long duration = System.currentTimeMillis() - startTime;
            log.error("Bootstrap failed after {} ms", duration, e);
            log.debug("State at failure – classLoader: {}, provider: {}, initialized: {}",
                    classLoader != null, provider != null, isInitialized);
            throw new RuntimeException("Bootstrap initialization failed!", e);
        }
    }

    /**
     * Launches the target game/application via the configured {@link GameProvider}.
     *
     * <p>Before delegation, this method optionally redirects {@link System#out} and {@link System#err}
     * to the SLF4J logger, unless the {@link  Constants#NO_REDIRECT_FLAG_NAME} argument is present.
     *
     * @param args command-line arguments to pass to the game provider; may be empty but not {@code null}
     * @throws RuntimeException if the provider's {@code launch} method throws any exception,
     *                          or if bootstrap state is inconsistent
     */
    @Override
    public void launch(String[] args) {
        if (!isInitialized) {
            log.warn("Bootstrap hasn't initialized yet! Use the 'init()' method first!");
            return;
        }

        if (isLaunched) {
            log.warn("GameProvider '{}' is now launched!", provider.getName());
            return;
        }

        try {
            isLaunched = true;

            if (!Arrays.asList(args).contains(Constants.NO_REDIRECT_FLAG_NAME)) {
                provider.redirectSystemStreamsToLogger();
            } else {
                log.info("The redirection of the default output of the game logs is disabled.");
            }

            provider.launch(args);
        } catch (Exception e) {
            isLaunched = false;
            throw new RuntimeException("Boostrap launch failed!", e);
        }
    }

    /**
     * Returns the active {@link GameProvider} instance.
     *
     * @return the initialized {@link GameProvider}, or {@code null} if {@link #init()} has not been called
     */
    @Override
    public GameProvider getGameProvider() {
        return provider;
    }

    /**
     * Returns the custom {@link BaseClassLoader} used by the Avrix.
     *
     * @return the initialized {@link BaseClassLoader}, or {@code null} if {@link #init()} has not been called
     */
    @Override
    public BaseClassLoader getClassLoader() {
        return classLoader;
    }

    /**
     * Returns the custom {@link PluginManager} used by the Avrix.
     *
     * @return The active {@link PluginManager} instance, or {@code null} if {@link #init()} has not been called
     */
    @Override
    public PluginManager getPluginManager() {
        return pluginManager;
    }

    /**
     * @return List of project authors/contributors. Never {@code null}.
     */
    @Override
    public List<String> getAuthors() {
        return List.of(Constants.LOADER_AUTHOR);
    }

    /**
     * @return Human-readable project name. Never {@code null}.
     */
    @Override
    public String getName() {
        return Constants.LOADER_NAME;
    }

    /**
     * @return Unique project identifier. Never {@code null}.
     */
    @Override
    public String getId() {
        return Constants.LOADER_ID;
    }

    /**
     * @return License name or SPDX identifier
     */
    @Override
    public String getLicense() {
        return Constants.LOADER_LICENSE;
    }

    /**
     * @return Contact information or URLs
     */
    @Override
    public List<String> getContacts() {
        return List.of(Constants.LOADER_CONTACTS);
    }

    /**
     * @return Current version string
     */
    @Override
    public String getVersion() {
        return Constants.LOADER_VERSION;
    }
}