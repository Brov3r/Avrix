package com.avrix.plugins;

import com.avrix.utils.VersionValidation;

/**
 * Validates semantic version constraint expressions used by plugin dependencies.
 */
public final class SemVerConstraints {

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
        if (!VersionValidation.isValidConstraint(constraint)) {
            throw new IllegalArgumentException("Invalid version constraint [" + constraint + "]");
        }
    }
}
