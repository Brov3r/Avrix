package com.avrix;

import com.avrix.core.KnotClassLoader;
import com.avrix.core.ServiceManager;
import com.avrix.mixin.MixinTransformer;
import com.avrix.plugins.DefaultPluginManager;
import com.avrix.plugins.PluginManager;
import com.avrix.provider.DefaultLoaderProvider;
import com.avrix.provider.GameProvider;
import com.avrix.provider.LoaderProvider;
import com.avrix.provider.ZomboidGameProvider;
import com.avrix.utils.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Main bootstrap entrypoint class for the Avrix plugin and mixin loader.
 * <p>
 * Orchestrates JVM classloader establishment, mixin subsystem configuration,
 * game provider discovery, plugin topological loading, and runtime delegation to Project Zomboid.
 */
public final class Main {

    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    /**
     * Prevents instantiation of the static bootstrap entrypoint.
     */
    private Main() {
        throw new UnsupportedOperationException("This is an entrypoint class and cannot be instantiated.");
    }

    /**
     * Bootstrap entrypoint invoked by the JVM.
     *
     * @param args command-line arguments passed to the JVM process
     */
    public static void main(String[] args) {
        long bootstrapStartTime = System.nanoTime();

        try {
            LOGGER.info("Starting Avrix Launcher (v{})...", Constants.LOADER_VERSION);

            String[] rawArgs = (args == null) ? new String[0] : args;
            Set<String> argumentsSet = new HashSet<>(Arrays.asList(rawArgs));

            // Initialize Unified Flat Knot ClassLoader
            KnotClassLoader classLoader = new KnotClassLoader(new URL[0], ClassLoader.getSystemClassLoader());
            Thread.currentThread().setContextClassLoader(classLoader);
            ServiceManager.register(KnotClassLoader.class, classLoader);

            // Initialize Mixin & Bytecode Transformation Subsystem
            MixinTransformer.init(classLoader);

            // Register Loader Provider & Game Provider
            LoaderProvider loaderProvider = new DefaultLoaderProvider();
            ServiceManager.register(LoaderProvider.class, loaderProvider);

            GameProvider gameProvider = new ZomboidGameProvider();
            ServiceManager.register(GameProvider.class, gameProvider);
            gameProvider.init();

            // Initialize Plugin Subsystem & Load Mod Graph
            PluginManager pluginManager = new DefaultPluginManager();
            ServiceManager.register(PluginManager.class, pluginManager);
            pluginManager.init();
            pluginManager.loadPlugins();

            // Configure System Log Streams Redirection
            if (argumentsSet.contains(Constants.NO_REDIRECT_FLAG_NAME)) {
                LOGGER.info("System log stream redirection is disabled via CLI argument: [{}]", Constants.NO_REDIRECT_FLAG_NAME);
            } else {
                gameProvider.redirectSystemStreamsToLogger();
            }

            // Calculate & Display Bootstrap Benchmark
            long bootstrapDurationMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - bootstrapStartTime);
            LOGGER.info("Avrix loader bootstrap completed in {}ms. Delegating control to {}...", bootstrapDurationMillis, gameProvider.getName());

            String[] gameLaunchArgs = filterLoaderArguments(rawArgs);
            gameProvider.launch(gameLaunchArgs);

        } catch (Throwable throwable) {
            LOGGER.error("A fatal error occurred during the application launch phase!", throwable);
            System.exit(1);
        }
    }

    /**
     * Filters out loader-specific command-line arguments before passing them to the game process.
     *
     * @param rawArgs original JVM command-line arguments
     * @return cleaned array containing only game-specific arguments
     */
    private static String[] filterLoaderArguments(String[] rawArgs) {
        if (rawArgs.length == 0) {
            return rawArgs;
        }

        Set<String> loaderSpecificFlags = Set.of(
                Constants.NO_REDIRECT_FLAG_NAME
        );

        List<String> gameArgs = new ArrayList<>(rawArgs.length);
        for (String arg : rawArgs) {
            if (!loaderSpecificFlags.contains(arg)) {
                gameArgs.add(arg);
            }
        }

        return gameArgs.toArray(String[]::new);
    }
}