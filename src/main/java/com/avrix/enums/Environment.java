package com.avrix.enums;

/**
 * An enumeration representing the execution environment, such as server, client, etc.
 */
public enum Environment {
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
    Environment(String value) {
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
     * Converts a string to the corresponding {@link Environment} enum value.
     *
     * @param text the string to convert
     * @return the corresponding {@link Environment} enum value. If the value could not be determined, it returns {@link Environment#BOTH}
     */
    public static Environment fromString(String text) {
        for (Environment env : Environment.values()) {
            if (env.value.equalsIgnoreCase(text)) {
                return env;
            }
        }
        return Environment.BOTH;
    }
}