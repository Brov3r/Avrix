package com.avrix;

/**
 * Main class designed to run various operations depending on the command line arguments passed.
 * Available operations:
 * - "-launch": launches the server through the {@link Launcher} class, passing command line arguments to it.
 * - "-install": starts the API installation process using the {@link Installer} class.
 * - "-uninstall": starts the process of uninstalling the API using the {@link Installer} class.
 * If an unknown flag is passed, an error message is printed.
 */
public class Main {
    /**
     * The main method that launches the application.
     *
     * @param args Command line arguments.
     */
    public static void main(String[] args) {
        switch (args[0]) {
            case "-launch" -> Launcher.launch(args);// Launch the server
            case "-install" -> Installer.install(); // Installing the API
            case "-uninstall" -> Installer.uninstall(); // Uninstalling the API
            default -> System.out.println("An unknown flag! Available flags: '-launch', '-install', '-uninstall'");
        }
    }
}