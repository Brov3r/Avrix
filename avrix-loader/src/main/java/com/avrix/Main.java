package com.avrix;

import org.tinylog.Logger;

/**
 * Application entry point for the Avrix runtime.
 */
public final class Main {
    /**
     * Bootstraps the Avrix runtime.
     *
     * @param args command-line arguments passed to the runtime; may be {@code null}
     */
    public static void main(String[] args) {
        try {
            LoaderManager.initialize();
            LoaderManager.launch(args);
        } catch (LoaderManager.LoaderException e) {
            // Initialization failure is fatal.
            Logger.error("Loader initialization failed: {}", e.getMessage());
            e.printStackTrace(System.err);
            System.exit(1);
        } catch (LoaderManager.ProviderLaunchException e) {
            // Launch failure is fatal.
            Logger.error("Runtime launch failed: {} ", e.getMessage());
            e.printStackTrace(System.err);
            System.exit(2);
        } catch (RuntimeException e) {
            // Unexpected failure.
            Logger.error("Unexpected fatal error: {}", e.getMessage());
            e.printStackTrace(System.err);
            System.exit(3);
        }
    }
}
