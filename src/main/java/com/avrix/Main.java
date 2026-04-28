package com.avrix;

import com.avrix.core.Bootstrap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Application entry point for the Avrix loader.
 * Delegates initialization and launch to the {@link Bootstrap} lifecycle manager.
 */
public final class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);

    /**
     * Starts the loader and launches the target application.
     *
     * @param args command-line arguments passed to the launcher
     */
    public static void main(String[] args) {
        try {
            Bootstrap.init();
            Bootstrap.launch(args);
        } catch (Exception e) {
            log.error("The application could not be launched!", e);
            System.exit(1);
        }
    }
}