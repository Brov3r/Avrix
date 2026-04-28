package com.avrix.core;

import com.avrix.mixin.MixinAgentBootstrap;
import com.avrix.plugins.PluginManager;
import com.avrix.provider.GameProvider;
import com.avrix.provider.ZomboidGameProvider;
import com.avrix.utils.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.Arrays;

/**
 * The central bootstrap class responsible for initializing and launching the Avrix loader.
 */
public class Bootstrap {
    private static final Logger log = LoggerFactory.getLogger(Bootstrap.class);

    /**
     * Custom class loader for isolating loader and plugin classes.
     */
    private static BaseClassLoader classLoader;

    /**
     * Active game provider instance.
     */
    private static GameProvider provider;

    private static boolean isLaunched = false;
    private static boolean isInitialized = false;

    /**
     * Initializes the Avrix framework environment.
     * <p>If initialization fails, all mutable state is rolled back to pre-init values
     * and a {@link RuntimeException} is thrown.
     *
     * @throws IllegalStateException if this method is called after successful initialization
     *                               (idempotency safeguard; logs warning and returns instead)
     * @throws RuntimeException      if any step in the bootstrap sequence fails,
     *                               wrapping the original cause
     */
    public static void init() {
        if (isInitialized) {
            log.warn("Bootstrap already initialized – skipping");
            return;
        }

        long startTime = System.currentTimeMillis();
        log.info("Bootstrap starting...");

        try {
            classLoader = new BaseClassLoader(new URL[0], ClassLoader.getSystemClassLoader());
            provider = new ZomboidGameProvider();

            MixinAgentBootstrap.loadAgent();

            provider.initialize(classLoader);

            PluginManager.init();
            PluginManager.loadPlugins();

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
    public static void launch(String[] args) {
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
     * <p>Useful for external modules needing direct access to provider-specific APIs.
     *
     * @return the initialized {@link GameProvider}, or {@code null} if {@link #init()} has not been called
     */
    public static GameProvider getGameProvider() {
        return provider;
    }

    /**
     * Returns the custom {@link BaseClassLoader} used by the framework.
     *
     * <p>Required for loading plugin classes, resources, or performing reflection
     * within the isolated classpath.
     *
     * @return the initialized {@link BaseClassLoader}, or {@code null} if {@link #init()} has not been called
     */
    public static BaseClassLoader getClassLoader() {
        return classLoader;
    }
}