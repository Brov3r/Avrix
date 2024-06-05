package com.avrix.plugin;

/**
 * Enum representing the environment in which a plugin can operate.
 */
public enum PluginEnvironment {
    /**
     * Client environment.
     */
    CLIENT("client"),

    /**
     * Server environment.
     */
    SERVER("server"),

    /**
     * Both client and server environments.
     */
    BOTH("both");

    private final String value;

    /**
     * Constructor to initialize the environment with a specific value.
     *
     * @param value the string representation of the environment
     */
    PluginEnvironment(String value) {
        this.value = value;
    }

    /**
     * Gets the string representation of the environment.
     *
     * @return the string representation of the environment
     */
    public String getValue() {
        return value;
    }

    /**
     * Converts a string to the corresponding {@code PluginEnvironment} enum value.
     *
     * @param text the string to convert
     * @return the corresponding {@code PluginEnvironment} enum value
     * @throws IllegalArgumentException if the string does not match any enum value
     */
    public static PluginEnvironment fromString(String text) {
        for (PluginEnvironment env : PluginEnvironment.values()) {
            if (env.value.equalsIgnoreCase(text)) {
                return env;
            }
        }
        throw new IllegalArgumentException("[!] Could not determine environment from value '" + text + "'!");
    }
}