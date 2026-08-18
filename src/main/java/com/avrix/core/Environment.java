package com.avrix.core;

import java.util.Locale;
import java.util.Objects;

/**
 * An enumeration representing the execution {@link Environment}, such as server, client, or both.
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
     * Checks whether this target environment constraint is compatible with the active runtime environment.
     *
     * @param activeEnvironment the active running environment, cannot be null
     * @return {@code true} if this environment matches or is {@link #BOTH}; {@code false} otherwise
     * @throws NullPointerException if {@code activeEnvironment} is null
     */
    public boolean isCompatibleWith(Environment activeEnvironment) {
        Objects.requireNonNull(activeEnvironment, "Active environment cannot be null");
        return this == BOTH || this == activeEnvironment;
    }

    /**
     * Converts a string to the corresponding {@link Environment} enum value.
     * Maps {@code "*"} and unknown values to {@link Environment#BOTH}.
     *
     * @param text the string to convert
     * @return the corresponding {@link Environment} enum value, or {@link Environment#BOTH} if undetermined
     */
    public static Environment fromString(String text) {
        if (text == null || text.isBlank()) {
            return BOTH;
        }

        String normalized = text.trim().toLowerCase(Locale.ROOT);

        return switch (normalized) {
            case "client" -> CLIENT;
            case "server" -> SERVER;
            default -> BOTH;
        };
    }
}