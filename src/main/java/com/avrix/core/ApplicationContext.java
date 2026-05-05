package com.avrix.core;

import com.avrix.plugins.PluginManager;
import com.avrix.provider.GameProvider;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Thread-safe global registry for the loader lifecycle.
 * Provides static access to interface contracts, enabling plugin compatibility across custom implementations.
 *
 * @see Bootstrap
 * @see PluginManager
 * @see GameProvider
 */
public final class ApplicationContext {

    private static final AtomicReference<ApplicationContext> INSTANCE = new AtomicReference<>();

    private final Bootstrap bootstrap;
    private final GameProvider gameProvider;
    private final PluginManager pluginManager;

    private ApplicationContext(Bootstrap bootstrap) {
        this.bootstrap = Objects.requireNonNull(bootstrap, "Bootstrap must not be null");
        this.gameProvider = Objects.requireNonNull(bootstrap.getGameProvider(), "GameProvider must not be null");
        this.pluginManager = Objects.requireNonNull(bootstrap.getPluginManager(), "PluginManager must not be null");
    }

    /**
     * Initializes the global context with the provided bootstrap.
     *
     * @param bootstrap core lifecycle manager to register
     * @throws IllegalStateException if the context is already initialized
     */
    public static void init(Bootstrap bootstrap) {
        if (!INSTANCE.compareAndSet(null, new ApplicationContext(bootstrap))) {
            throw new IllegalStateException("Application context is already initialized");
        }
    }

    /**
     * Retrieves the globally initialized context.
     *
     * @return the active application context
     * @throws IllegalStateException if init(Bootstrap) has not been invoked
     */
    public static ApplicationContext getInstance() {
        ApplicationContext ctx = INSTANCE.get();
        if (ctx == null) {
            throw new IllegalStateException("Application context is not initialized. Invoke init(Bootstrap) at startup.");
        }
        return ctx;
    }

    /**
     * @return the registered bootstrap instance
     */
    public Bootstrap getBootstrap() {
        return bootstrap;
    }

    /**
     * @return the active game provider
     */
    public GameProvider getGameProvider() {
        return gameProvider;
    }

    /**
     * @return the active plugin manager
     */
    public PluginManager getPluginManager() {
        return pluginManager;
    }
}