package com.avrix.plugins;

import com.github.zafarkhaja.semver.Version;

/**
 * Validates semantic version constraint expressions used by plugin dependencies.
 */
public final class SemVerConstraints {

    private static final Version PROBE_VERSION = Version.parse("0.0.0");

    /**
     * Prevents instantiation of this utility class.
     *
     * @throws UnsupportedOperationException always
     */
    private SemVerConstraints() {
        throw new UnsupportedOperationException("SemVerConstraints utility class cannot be instantiated");
    }

    /**
     * Validates the syntax of a dependency version constraint.
     *
     * @param constraint constraint expression to validate
     * @throws IllegalArgumentException if the constraint syntax is invalid
     */
    public static void validate(String constraint) {
        try {
            PROBE_VERSION.satisfies(constraint);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Invalid version constraint [" + constraint + "]", e);
        }
    }
}
