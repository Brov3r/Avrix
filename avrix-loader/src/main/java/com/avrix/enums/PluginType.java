package com.avrix.enums;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Defines the category of a runtime component in the Avrix ecosystem.
 */
public enum PluginType {

    /**
     * Core runtime loader.
     */
    LOADER("loader"),

    /**
     * Game provider implementation.
     */
    PROVIDER("provider"),


    /**
     * Regular game plugin.
     */
    PLUGIN("plugin");

    private static final Map<String, PluginType> BY_VALUE =
            Arrays.stream(values()).collect(Collectors.toUnmodifiableMap(PluginType::getValue, t -> t));

    private final String value;

    /**
     * Creates a plugin type with its stable string value.
     *
     * @param value stable lowercase value used in metadata/configuration
     * @throws NullPointerException if {@code value} is {@code null}
     */
    PluginType(String value) {
        this.value = Objects.requireNonNull(value, "PluginType value cannot be null");
    }

    /**
     * Returns the stable string value of this type.
     *
     * <p>
     * This value is used in configuration/metadata files (e.g. YAML).
     * </p>
     *
     * @return stable string value (e.g. {@code "plugin"}), never {@code null}
     */
    public String getValue() {
        return value;
    }

    /**
     * Resolves a {@link PluginType} from its stable string value.
     *
     * <p>
     * The input is normalized by trimming whitespace and converting to lowercase
     * using {@link Locale#ROOT}. This makes parsing more robust for configuration files.
     * </p>
     *
     * @param value string value to parse; must not be {@code null} or blank
     * @return matching {@link PluginType}
     * @throws NullPointerException     if {@code value} is {@code null}
     * @throws IllegalArgumentException if {@code value} is blank or does not match any known type
     */
    public static PluginType fromValue(String value) {
        Objects.requireNonNull(value, "Value cannot be null");

        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Value must not be blank.");
        }

        PluginType type = BY_VALUE.get(normalized);
        if (type != null) {
            return type;
        }

        throw new IllegalArgumentException(
                String.format("No PluginType found for value '%s'. Valid values are: %s",
                        value,
                        Arrays.stream(values())
                                .map(PluginType::getValue)
                                .collect(Collectors.joining(", ", "[", "]"))
                )
        );
    }

    /**
     * Indicates whether this type is part of the core runtime bootstrap.
     *
     * @return {@code true} for {@link #LOADER} and {@link #PROVIDER}, otherwise {@code false}
     */
    public boolean isCoreType() {
        return this == LOADER || this == PROVIDER;
    }

    /**
     * Indicates whether this type represents an optional runtime extension.
     *
     * @return {@code true} for {@link #PLUGIN}, otherwise {@code false}
     */
    public boolean isOptionalType() {
        return this == PLUGIN;
    }

    /**
     * Returns a human-friendly name for UI/logging purposes.
     *
     * @return display name, never {@code null}
     */
    public String getDisplayName() {
        return switch (this) {
            case LOADER -> "System Loader";
            case PROVIDER -> "Game Provider";
            case PLUGIN -> "Game Plugin";
        };
    }

    /**
     * Returns a short description of the type purpose.
     *
     * @return description, never {@code null}
     */
    public String getDescription() {
        return switch (this) {
            case LOADER -> "Loads and manages the plugin system infrastructure";
            case PROVIDER -> "Provides game-specific implementations and resources";
            case PLUGIN -> "Extends or modifies game behavior and features";
        };
    }
}