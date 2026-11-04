package com.avrix.loader;

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
import com.avrix.provider.GameProvider;
import com.avrix.provider.LoaderProvider;

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

    /**
     * Prevents instantiation of this static factory.
     */
    private LoaderFactory() {
        throw new UnsupportedOperationException("This is a factory class and cannot be instantiated.");
    }

    /**
     * Creates and initializes the default loader runtime.
     *
     * @param config loader configuration
     * @return initialized loader context
     * @throws NullPointerException     if {@code config} is null
     * @throws IllegalStateException    if a required service cannot be initialized
     */
    public static LoaderContext create(LoaderConfig config) {
        Objects.requireNonNull(config, "Loader config cannot be null");

        KnotClassLoader classLoader = new KnotClassLoader(new URL[0], ClassLoader.getSystemClassLoader());
        Thread.currentThread().setContextClassLoader(classLoader);
        ServiceManager.register(KnotClassLoader.class, classLoader);

        MixinTransformer mixinTransformer = new MixinTransformer(classLoader);
        classLoader.setClassTransformer(mixinTransformer);

        LoaderProvider loaderProvider = new DefaultLoaderProvider();
        ServiceManager.register(LoaderProvider.class, loaderProvider);

        GameProvider gameProvider = GameProviderFactory.load(config.providerId(), classLoader);
        ServiceManager.register(GameProvider.class, gameProvider);
        gameProvider.init(classLoader);

        Environment resolvedEnvironment = config.environment() == Environment.BOTH
                ? gameProvider.getEnvironment()
                : config.environment();

        PluginManager pluginManager = new DefaultPluginManager(
                config.withEnvironment(resolvedEnvironment),
                classLoader,
                gameProvider,
                loaderProvider,
                mixinTransformer,
                new PluginScanner(config),
                new PluginManifestParser(config.strict())
        );
        ServiceManager.register(PluginManager.class, pluginManager);

        return new LoaderContext(classLoader, gameProvider, pluginManager);
    }
}
