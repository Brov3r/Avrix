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
     * Normalizes the version string, leaving only numbers, dashes, periods and underscores.
     * Removes the last character if it is not a number.
     *
     * @param version The version string to normalize.
     * @return The normalized version string.
     */
    public static String normalizeVersion(String version) {
        if (version == null || version.isEmpty()) {
            return "";
        }

        // We remove all characters except numbers, dots, dashes and underscores
        String normalized = version.replaceAll("[^\\d\\-\\.\\_\\,]", "");

        // Replace all dashes and underscores with dots
        normalized = normalized.replaceAll("[\\-\\_\\,]", ".");

        // Removing all duplicate points
        normalized = normalized.replaceAll("\\.+", ".");

        if (!normalized.isEmpty() && !Character.isDigit(normalized.charAt(normalized.length() - 1))) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }

        String[] parts = normalized.split("\\.");
        StringBuilder versionBuilder = new StringBuilder();

        for (String part : parts) {
            if (part.isEmpty() || part.equals("0")) {
                versionBuilder.append("0");
            } else {
                versionBuilder.append(part.replaceFirst("^0+(?!$)", ""));
            }
            versionBuilder.append(".");
        }

        if (!versionBuilder.isEmpty()) {
            versionBuilder.setLength(versionBuilder.length() - 1);
        }

        return versionBuilder.toString();
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
     * <p>
     * The comparison is based on the normalized versions, where non-numeric characters
     * except for dots (.), dashes (-), and underscores (_) are removed, and
     * dashes and underscores are replaced by dots. If a version segment is empty,
     * it is considered as zero (0). If the last character of a version is not a digit,
     * it is removed.
     *
     * @param versionOne the first version string to compare.
     * @param versionTwo the second version string to compare.
     * @return {@code 0} if both versions are equal, a value less than {@code 0}
     * if {@code versionOne} is less than {@code versionTwo}, and a value
     * greater than {@code 0} if {@code versionOne} is greater than {@code versionTwo}.
     */
    public static int compareVersions(String versionOne, String versionTwo) {
        String[] v1Parts = normalizeVersion(versionOne).split("\\.");
        String[] v2Parts = normalizeVersion(versionTwo).split("\\.");

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