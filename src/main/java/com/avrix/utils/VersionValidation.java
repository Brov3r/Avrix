package com.avrix.utils;

import com.github.zafarkhaja.semver.Version;

/**
 * Shared semantic-version and version-constraint validation.
 *
 * <p><b>API Note:</b> This is a stateless utility class and cannot be instantiated.
 */
public final class VersionValidation {

    private static final Version PROBE_VERSION = Version.parse("0.0.0");

    /**
     * Prevents instantiation of this utility class.
     *
     * @throws UnsupportedOperationException always
     */
    private VersionValidation() {
        throw new UnsupportedOperationException("VersionValidation utility class cannot be instantiated");
    }

    /**
     * Checks whether a value is a valid semantic version.
     *
     * @param version version string to validate, may be {@code null}
     * @return {@code true} when the value is a valid SemVer string
     */
    public static boolean isValidVersion(String version) {
        if (version == null || version.isBlank()) {
            return false;
        }
        try {
            Version.parse(version.strip());
            return true;
        } catch (RuntimeException _) {
            return false;
        }
    }

    /**
     * Checks whether a value is a valid semantic-version constraint expression.
     *
     * @param constraint constraint expression to validate, may be {@code null}
     * @return {@code true} when the constraint syntax is valid
     */
    public static boolean isValidConstraint(String constraint) {
        if (constraint == null || constraint.isBlank()) {
            return false;
        }
        try {
            PROBE_VERSION.satisfies(constraint.strip());
            return true;
        } catch (RuntimeException _) {
            return false;
        }
    }
}
