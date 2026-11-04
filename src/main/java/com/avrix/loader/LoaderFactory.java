package com.avrix.loader;

import com.avrix.api.GameProvider;
import com.avrix.api.LoaderProvider;
import com.avrix.api.PluginApprovalGate;
import com.avrix.core.Environment;
import com.avrix.core.KnotClassLoader;
import com.avrix.core.LoaderConfig;
import com.avrix.core.ServiceManager;
import com.avrix.mixin.MixinTransformer;
import com.avrix.plugins.DefaultPluginManager;
import com.avrix.plugins.PluginManager;
import com.avrix.plugins.PluginManifestParser;
import com.avrix.plugins.PluginScanner;
import com.avrix.provider.DefaultLoaderProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;

/**
 * Creates the default loader runtime and wires its services.
 * <p>
 * This class is the composition root for the default Avrix implementation. Game providers are discovered through
 * {@link GameProviderFactory} and {@link java.util.ServiceLoader}, so alternative implementations can be added
 * without changing the bootstrap entrypoint.
 */
public final class LoaderFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoaderFactory.class);

    /**
     * Prevents instantiation of this static factory.
     */
    private LoaderFactory() {
        throw new UnsupportedOperationException("This is a factory class and cannot be instantiated.");
    }

    /**
     * Creates and initializes the default loader runtime without game arguments.
     *
     * @param config loader configuration
     * @return initialized loader context
     * @throws NullPointerException  if {@code config} is null
     * @throws IllegalStateException if a required service cannot be initialized
     */
    public static LoaderContext create(LoaderConfig config) {
        return create(config, new String[0]);
    }

    /**
     * Creates and initializes the default loader runtime.
     *
     * @param config  loader configuration
     * @param rawArgs command-line arguments passed to the game
     * @return initialized loader context
     * @throws NullPointerException  if {@code config} is null
     * @throws IllegalStateException if a required service cannot be initialized
     */
    public static LoaderContext create(LoaderConfig config, String[] rawArgs) {
        Objects.requireNonNull(config, "Loader config cannot be null");

        String[] launchArgs = (rawArgs == null) ? new String[0] : rawArgs.clone();
        ClassLoader previousContextClassLoader = Thread.currentThread().getContextClassLoader();
        KnotClassLoader classLoader = new KnotClassLoader(new URL[0], ClassLoader.getSystemClassLoader());

        boolean knotRegistered = false;
        LoaderProvider loaderProvider = null;
        boolean loaderProviderRegistered = false;
        GameProvider gameProvider = null;
        boolean gameProviderRegistered = false;
        PluginManager pluginManager = null;
        boolean pluginManagerRegistered = false;

        try {
            Thread.currentThread().setContextClassLoader(classLoader);
            ServiceManager.register(KnotClassLoader.class, classLoader);
            knotRegistered = true;

            MixinTransformer mixinTransformer = new MixinTransformer(classLoader, config.strict());
            classLoader.setStrictTransformationFailures(config.strict());
            classLoader.setClassTransformer(mixinTransformer);

            loaderProvider = new DefaultLoaderProvider();
            ServiceManager.register(LoaderProvider.class, loaderProvider);
            loaderProviderRegistered = true;

            gameProvider = GameProviderFactory.load(config.providerId(), classLoader);
            ServiceManager.register(GameProvider.class, gameProvider);
            gameProviderRegistered = true;
            gameProvider.configure(launchArgs);
            gameProvider.init(classLoader);

            Environment resolvedEnvironment = config.environment() == Environment.AUTO
                    ? gameProvider.getEnvironment()
                    : config.environment();

            PluginApprovalGate approvalGate = gameProvider.createApprovalGate(
                    gameProvider.getCacheRoot(),
                    resolvedEnvironment
            );

            pluginManager = new DefaultPluginManager(
                    config.withEnvironment(resolvedEnvironment),
                    classLoader,
                    gameProvider,
                    loaderProvider,
                    mixinTransformer,
                    new PluginScanner(config, gameProvider.getPluginSources(config.strict())),
                    new PluginManifestParser(),
                    approvalGate
            );
            ServiceManager.register(PluginManager.class, pluginManager);
            pluginManagerRegistered = true;

            return new LoaderContext(classLoader, gameProvider, loaderProvider, pluginManager);
        } catch (RuntimeException | Error e) {
            closeQuietly(pluginManager);
            closeQuietly(gameProvider);

            if (pluginManagerRegistered) {
                ServiceManager.unregister(PluginManager.class);
            }
            if (gameProviderRegistered) {
                ServiceManager.unregister(GameProvider.class);
            }
            if (loaderProviderRegistered) {
                ServiceManager.unregister(LoaderProvider.class);
            }
            if (knotRegistered) {
                ServiceManager.unregister(KnotClassLoader.class);
            }

            try {
                classLoader.close();
            } catch (IOException closeFailure) {
                LOGGER.debug("Failed to close classloader after loader factory failure", closeFailure);
            }

            Thread.currentThread().setContextClassLoader(previousContextClassLoader);
            throw e;
        }
    }

    private static void closeQuietly(PluginManager pluginManager) {
        if (pluginManager == null) {
            return;
        }
        try {
            pluginManager.close();
        } catch (Throwable throwable) {
            LOGGER.debug("Failed to close plugin manager after loader factory failure", throwable);
        }
    }

    private static void closeQuietly(GameProvider gameProvider) {
        if (gameProvider == null) {
            return;
        }
        try {
            gameProvider.close();
        } catch (Throwable throwable) {
            LOGGER.debug("Failed to close game provider after loader factory failure", throwable);
        }
    }
}
