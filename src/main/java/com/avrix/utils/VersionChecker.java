package com.avrix.utils;

/**
 * A set of tools for checking the compatibility of two versions.
 */
public class VersionChecker {
    /**
     * Checks whether a version meets a given condition.
     *
     * @param currentVersion   The current version for comparison, for example, "41.78.16".
     * @param versionCondition Version condition, for example, ">=41.78.16".
     * @return true if the current version matches the condition.
     */
    public static boolean isVersionCompatible(String currentVersion, String versionCondition) {
        ComparisonOperator operator = extractOperator(versionCondition);
        String requiredVersion = normalizeVersion(versionCondition.substring(operator.symbol.length()));

        int comparisonResult = compareVersions(normalizeVersion(currentVersion), requiredVersion);
        return switch (operator) {
            case GREATER_THAN -> comparisonResult > 0;
            case GREATER_THAN_OR_EQUAL -> comparisonResult >= 0;
            case LESS_THAN -> comparisonResult < 0;
            case LESS_THAN_OR_EQUAL -> comparisonResult <= 0;
            case EQUAL, UNKNOWN -> comparisonResult == 0;
        };
    }

    /**
     * Extracts the comparison operator from a version condition.
     *
     * @param condition The version condition string, e.g., ">=41.78.16".
     * @return The extracted ComparisonOperator.
     */
    private static ComparisonOperator extractOperator(String condition) {
        if (condition.startsWith(">=")) return ComparisonOperator.GREATER_THAN_OR_EQUAL;
        if (condition.startsWith(">")) return ComparisonOperator.GREATER_THAN;
        if (condition.startsWith("<=")) return ComparisonOperator.LESS_THAN_OR_EQUAL;
        if (condition.startsWith("<")) return ComparisonOperator.LESS_THAN;
        if (condition.startsWith("=")) return ComparisonOperator.EQUAL;
        return ComparisonOperator.UNKNOWN;
    }

    /**
     * Normalizes a version string by removing any non-numeric suffixes.
     *
     * @param version The version string to normalize.
     * @return The normalized version string.
     */
    private static String normalizeVersion(String version) {
        return version.replace("-", ".").replace(",", ".").split("[^\\d.]")[0];
    }

    /**
     * Enumeration of comparison operators for version checking.
     */
    private enum ComparisonOperator {
        GREATER_THAN(">"), GREATER_THAN_OR_EQUAL(">="),
        LESS_THAN("<"), LESS_THAN_OR_EQUAL("<="),
        EQUAL("="), UNKNOWN("");

        final String symbol;

        ComparisonOperator(String symbol) {
            this.symbol = symbol;
        }
    }

    /**
     * Compares two version strings.
     *
     * @param versionOne Version 1.
     * @param versionTwo Version 2.
     * @return 0, if v1 == v2; value < 0 if v1 < v2; value > 0 if v1 > v2.
     */
    private static int compareVersions(String versionOne, String versionTwo) {
        String[] v1Parts = versionOne.split("\\.");
        String[] v2Parts = versionTwo.split("\\.");

        int length = Math.max(v1Parts.length, v2Parts.length);
        for (int i = 0; i < length; i++) {
            int v1Part = i < v1Parts.length ? Integer.parseInt(v1Parts[i]) : 0;
            int v2Part = i < v2Parts.length ? Integer.parseInt(v2Parts[i]) : 0;

            if (v1Part != v2Part) {
                return v1Part - v2Part;
            }
        }
        return 0;
    }
}