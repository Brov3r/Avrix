package com.avrix;

import com.avrix.core.AvrixBootstrap;
import com.avrix.core.BaseClassLoader;
import com.avrix.core.Bootstrap;
import com.avrix.plugins.AvrixPluginManager;
import com.avrix.provider.ZomboidGameProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;

/**
 * Application entry point for the Avrix loader.
 * Delegates initialization and launch to the {@link AvrixBootstrap} lifecycle manager.
 */
public final class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);

    /**
     * Starts the loader and launches the target application.
     *
     * @param args command-line arguments passed to the launcher
     */
    public static void main(String[] args) {
        Bootstrap bootstrap = new AvrixBootstrap(new BaseClassLoader(new URL[0], ClassLoader.getSystemClassLoader()),
                new AvrixPluginManager(), new ZomboidGameProvider());
        try {
            bootstrap.init();
            bootstrap.launch(args);
        } catch (Exception e) {
            log.error("The application could not be launched!", e);
            System.exit(1);
        }
    }
}