package com.avrix.loader;

import com.avrix.core.KnotClassLoader;
import com.avrix.core.ServiceManager;
import com.avrix.plugins.PluginManager;
import com.avrix.provider.GameProvider;
import com.avrix.provider.LoaderProvider;
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
    private final LoaderProvider loaderProvider;
    private final PluginManager pluginManager;
    private final AtomicBoolean closed = new AtomicBoolean();

    /**
     * Constructs a new loader context.
     *
     * @param classLoader    flat classloader hosting the runtime
     * @param gameProvider   active game provider
     * @param loaderProvider active loader metadata provider, may be {@code null}
     * @param pluginManager  active plugin manager
     * @throws NullPointerException if {@code classLoader}, {@code gameProvider}, or {@code pluginManager} is null
     */
    public LoaderContext(
            KnotClassLoader classLoader,
            GameProvider gameProvider,
            LoaderProvider loaderProvider,
            PluginManager pluginManager
    ) {
        this.classLoader = Objects.requireNonNull(classLoader, "KnotClassLoader cannot be null");
        this.gameProvider = Objects.requireNonNull(gameProvider, "GameProvider cannot be null");
        this.loaderProvider = loaderProvider;
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
     * The Project Zomboid client and dedicated server main methods block until exit, so closing after
     * {@link #launch(String[])} returns is safe.
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

        unregisterIfSame(PluginManager.class, pluginManager);
        unregisterIfSame(GameProvider.class, gameProvider);
        unregisterIfSame(LoaderProvider.class, loaderProvider);
        unregisterIfSame(KnotClassLoader.class, classLoader);
    }

    /**
     * Removes a loader-owned service registration only when it still points to the provided instance.
     *
     * @param serviceType service contract type
     * @param instance    instance owned by this context
     * @param <T>         service type
     */
    private static <T> void unregisterIfSame(Class<T> serviceType, T instance) {
        ServiceManager.find(serviceType)
                .filter(service -> service == instance)
                .ifPresent(service -> ServiceManager.unregister(serviceType));
    }
}
