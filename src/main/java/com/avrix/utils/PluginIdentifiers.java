package com.avrix.utils;

import javax.lang.model.SourceVersion;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Shared validation rules for plugin identifiers and Java class names.
 *
 * <p><b>API Note:</b> This is a stateless utility class and cannot be instantiated.
 */
public final class PluginIdentifiers {

    /**
     * Canonical plugin identifier pattern.
     */
    public static final Pattern PATTERN = Pattern.compile("[a-z0-9][a-z0-9._-]{0,63}");

    /**
     * Prevents instantiation of this utility class.
     *
     * @throws UnsupportedOperationException always
     */
    private PluginIdentifiers() {
        throw new UnsupportedOperationException("PluginIdentifiers utility class cannot be instantiated");
    }

    /**
     * Checks whether a value is a valid canonical plugin identifier.
     *
     * @param identifier identifier to validate, may be {@code null}
     * @return {@code true} when the identifier matches the canonical pattern
     */
    public static boolean isValid(String identifier) {
        if (identifier == null) {
            return false;
        }

        String normalized = identifier.strip();
        if (normalized.isEmpty()) {
            return false;
        }
        if (normalized.chars().anyMatch(Character::isWhitespace)) {
            return false;
        }
        if (normalized.chars().anyMatch(Character::isISOControl)) {
            return false;
        }
        return PATTERN.matcher(normalized).matches();
    }

    /**
     * Validates a required plugin identifier and throws a descriptive exception when it is invalid.
     *
     * @param identifier identifier to validate, cannot be {@code null}
     * @param field      human-readable field name used in the exception message
     * @throws NullPointerException     if {@code identifier} is {@code null}
     * @throws IllegalArgumentException if the identifier is blank, contains whitespace or control characters,
     *                                  or does not match the canonical pattern
     */
    public static void requireValid(String identifier, String field) {
        Objects.requireNonNull(identifier, field + " cannot be null");

        String normalized = identifier.strip();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(field + " cannot be blank");
        }
        if (normalized.chars().anyMatch(Character::isWhitespace)) {
            throw new IllegalArgumentException(field + " cannot contain whitespace");
        }
        if (normalized.chars().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException(field + " cannot contain control characters");
        }
        if (!PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException(
                    field + " value [" + normalized + "] must match identifier pattern " + PATTERN.pattern()
            );
        }
    }

    /**
     * Checks whether a value is a valid fully qualified Java class name.
     *
     * @param className class name to validate, may be {@code null}
     * @return {@code true} when the value is a valid Java name
     */
    public static boolean isValidJavaName(String className) {
        return className != null && !className.isBlank() && SourceVersion.isName(className);
    }
}
