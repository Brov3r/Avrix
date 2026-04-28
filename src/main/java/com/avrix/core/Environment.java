package com.avrix.core;

/**
 * An enumeration representing the execution {@link Environment}, such as server, client or both
 */
public enum Environment {
    /**
     * Client {@link Environment}.
     */
    CLIENT("client"),

    /**
     * Server {@link Environment}.
     */
    SERVER("server"),

    /**
     * Both client and server {@link Environment}.
     */
    BOTH("both");

    private final String value;

    /**
     * Constructor to initialize the {@link Environment} with a specific value.
     *
     * @param value the string representation of the {@link Environment}
     */
    Environment(String value) {
        this.value = value;
    }

    /**
     * Gets the string representation of the {@link Environment}.
     *
     * @return the string representation of the {@link Environment}
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