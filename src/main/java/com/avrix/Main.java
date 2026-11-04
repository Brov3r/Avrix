package com.avrix;

import com.avrix.core.Environment;
import com.avrix.core.LoaderConfig;
import com.avrix.core.RuntimeMarker;
import com.avrix.loader.LoaderContext;
import com.avrix.loader.LoaderFactory;
import com.avrix.utils.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Main bootstrap entrypoint for the Avrix plugin and mixin loader.
 * <p>
 * Parses command-line arguments, builds the loader configuration, creates the runtime through {@link LoaderFactory},
 * and delegates game startup to the resulting {@link LoaderContext}.
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

            String serverName = parseServerName(rawArgs);
            if (serverName != null && !serverName.isBlank()) {
                System.setProperty(Constants.SERVER_NAME_PROPERTY, serverName);
            }

            String cacheDirectory = parseCacheDirectory(rawArgs);
            if (cacheDirectory != null) {
                System.setProperty(Constants.CACHE_DIR_PROPERTY, cacheDirectory);
            }

            String providerId = System.getProperty(
                    Constants.GAME_PROVIDER_PROPERTY,
                    Constants.DEFAULT_GAME_PROVIDER_ID
            );

            LoaderConfig config = new LoaderConfig(
                    Path.of(Constants.PLUGINS_FOLDER_NAME),
                    Constants.METADATA_NAME,
                    Constants.PLUGIN_EXTENSION,
                    Constants.PLUGIN_ICON_NAME,
                    providerId,
                    Environment.AUTO,
                    !argumentsSet.contains(Constants.NO_REDIRECT_FLAG_NAME),
                    argumentsSet.contains(Constants.STRICT_FLAG_NAME)
            );

            LoaderContext context = LoaderFactory.create(config);
            Runtime.getRuntime().addShutdownHook(new Thread(context::close, "avrix-shutdown"));

            List<Path> runtimeMarkers = context.requiresRuntimeMarker()
                    ? RuntimeMarker.activate()
                    : List.of();
            if (!runtimeMarkers.isEmpty()) {
                Runtime.getRuntime().addShutdownHook(
                        new Thread(() -> RuntimeMarker.deactivate(runtimeMarkers), "avrix-runtime-marker")
                );
            }

            try {
                context.initialize();

                if (!config.redirectSystemStreams()) {
                    LOGGER.info("System log stream redirection is disabled via CLI argument: [{}]", Constants.NO_REDIRECT_FLAG_NAME);
                } else {
                    context.redirectSystemStreams();
                }

                long bootstrapDurationMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - bootstrapStartTime);
                LOGGER.info("Avrix loader bootstrap completed in {}ms. Delegating control to {}...", bootstrapDurationMillis, context.getGameName());

                String[] gameLaunchArgs = filterLoaderArguments(rawArgs);
                context.launch(gameLaunchArgs);
            } finally {
                RuntimeMarker.deactivate(runtimeMarkers);
                context.close();
            }

        } catch (Throwable throwable) {
            LOGGER.error("A fatal error occurred during the application launch phase!", throwable);
            System.exit(1);
        }
    }

    /**
     * Extracts the dedicated-server name from the game arguments.
     *
     * @param args original JVM command-line arguments
     * @return server name, or {@code null} when no {@code -servername} argument is present
     */
    private static String parseServerName(String[] args) {
        for (int index = 0; index < args.length; index++) {
            String argument = args[index];
            if ("-servername".equalsIgnoreCase(argument) && index + 1 < args.length) {
                return args[index + 1];
            }

            String prefix = "-servername=";
            if (argument.regionMatches(true, 0, prefix, 0, prefix.length())) {
                return argument.substring(prefix.length());
            }
        }

        return null;
    }

    /**
     * Extracts the Project Zomboid cache directory override from the game arguments.
     *
     * @param args original JVM command-line arguments
     * @return last {@code -cachedir=} value, or {@code null} when the argument is absent
     */
    private static String parseCacheDirectory(String[] args) {
        String cacheDirectory = null;
        for (String argument : args) {
            if (argument != null && argument.startsWith(Constants.CACHE_DIR_ARGUMENT_PREFIX)) {
                cacheDirectory = argument.substring(Constants.CACHE_DIR_ARGUMENT_PREFIX.length()).trim();
            }
        }
        return cacheDirectory;
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
                Constants.NO_REDIRECT_FLAG_NAME,
                Constants.STRICT_FLAG_NAME
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