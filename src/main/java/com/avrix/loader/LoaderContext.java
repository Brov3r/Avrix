package com.avrix.loader;

import com.avrix.core.KnotClassLoader;
import com.avrix.plugins.PluginManager;
import com.avrix.provider.GameProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Runtime context assembled by {@link LoaderFactory}.
 * <p>
 * Exposes the loader lifecycle operations without exposing the concrete provider and plugin manager implementations
 * to the bootstrap entrypoint.
 */
public final class LoaderContext implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoaderContext.class);

    private final KnotClassLoader classLoader;
    private final GameProvider gameProvider;
    private final PluginManager pluginManager;
    private final AtomicBoolean closed = new AtomicBoolean();

    /**
     * Constructs a new loader context.
     *
     * @param classLoader   flat classloader hosting the runtime
     * @param gameProvider  active game provider
     * @param pluginManager active plugin manager
     * @throws NullPointerException if any dependency is null
     */
    public LoaderContext(KnotClassLoader classLoader, GameProvider gameProvider, PluginManager pluginManager) {
        this.classLoader = Objects.requireNonNull(classLoader, "KnotClassLoader cannot be null");
        this.gameProvider = Objects.requireNonNull(gameProvider, "GameProvider cannot be null");
        this.pluginManager = Objects.requireNonNull(pluginManager, "PluginManager cannot be null");
    }

    /**
     * Initializes the plugin manager and loads all discovered plugins.
     */
    public void initialize() {
        pluginManager.init();
        pluginManager.loadPlugins();
    }

    /**
     * Redirects the game output streams to the loader logging subsystem.
     */
    public void redirectSystemStreams() {
        gameProvider.redirectSystemStreamsToLogger();
    }

    /**
     * Launches the game with the provided arguments.
     *
     * @param args command-line arguments passed to the game
     */
    public void launch(String[] args) {
        gameProvider.launch(args);
    }

    /**
     * Returns the display name of the active game.
     *
     * @return game display name
     */
    public String getGameName() {
        return gameProvider.getName();
    }

    /**
     * Shuts down loaded plugins and closes the flat classloader.
     * <p>
     * This method is idempotent and safe to call from both a try-with-resources block and a JVM shutdown hook.
     */
    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }

        try {
            pluginManager.close();
        } catch (Throwable throwable) {
            LOGGER.error("Failed to close plugin manager", throwable);
        }

        try {
            gameProvider.close();
        } catch (Throwable throwable) {
            LOGGER.error("Failed to close game provider", throwable);
        }

        try {
            classLoader.close();
        } catch (IOException e) {
            LOGGER.warn("Failed to close KnotClassLoader", e);
        }
    }
}
