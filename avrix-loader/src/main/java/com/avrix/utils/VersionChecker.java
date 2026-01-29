package com.avrix.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Utility for normalizing and comparing version strings.
 *
 * <p>
 * Supported condition formats:
 * </p>
 * <ul>
 *   <li>{@code ">=41.78.16"}</li>
 *   <li>{@code "<= 41.78"}</li>
 *   <li>{@code "=41.78.16"} or {@code "==41.78.16"}</li>
 *   <li>{@code "41.78.16"} (no operator means equality)</li>
 * </ul>
 *
 * <p>
 * Version normalization keeps only numeric segments separated by dots. Common separators
 * ({@code '-'}, {@code '_'}, {@code ','}) are treated as dots. All other characters are ignored.
 * </p>
 */
public final class VersionChecker {

    private VersionChecker() {
        // Utility class
    }

    /**
     * Checks whether the given {@code currentVersion} satisfies the {@code versionCondition}.
     *
     * <p>
     * If the condition does not specify an operator, equality is assumed.
     * Both the current version and the required version are normalized using
     * {@link #normalizeVersion(String)} before comparison.
     * </p>
     *
     * @param currentVersion   current version string (e.g. {@code "41.78.16"}); must not be {@code null}
     * @param versionCondition version condition (e.g. {@code ">=41.78.16"}); must not be {@code null} or blank
     * @return {@code true} if compatible, otherwise {@code false}
     * @throws NullPointerException     if any argument is {@code null}
     * @throws IllegalArgumentException if {@code versionCondition} is blank or contains no version digits after normalization
     */
    public static boolean isVersionCompatible(String currentVersion, String versionCondition) {
        Objects.requireNonNull(currentVersion, "Current version must not be null");
        Objects.requireNonNull(versionCondition, "Version condition must not be null");

        String trimmed = versionCondition.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Version condition must not be blank.");
        }

        ParsedCondition parsed = parseCondition(trimmed);

        String normalizedCurrent = normalizeVersion(currentVersion);
        String normalizedRequired = normalizeVersion(parsed.requiredVersion());

        if (normalizedRequired.equals("0") && !containsAnyDigit(parsed.requiredVersion())) {
            throw new IllegalArgumentException("Version condition does not contain a valid version: " + versionCondition);
        }

        int comparison = compareNormalizedVersions(normalizedCurrent, normalizedRequired);
        ComparisonOperator op = parsed.operator() == ComparisonOperator.NONE ? ComparisonOperator.EQUAL : parsed.operator();

        return switch (op) {
            case GREATER_THAN -> comparison > 0;
            case GREATER_THAN_OR_EQUAL -> comparison >= 0;
            case LESS_THAN -> comparison < 0;
            case LESS_THAN_OR_EQUAL -> comparison <= 0;
            case EQUAL -> comparison == 0;
            default -> throw new IllegalStateException("Unexpected value: " + op);
        };
    }

    /**
     * Normalizes a version string into a dot-separated numeric form.
     *
     * <p>
     * Rules:
     * </p>
     * <ul>
     *   <li>Only digits are kept as part of numeric segments.</li>
     *   <li>{@code '.'}, {@code '-'}, {@code '_'}, {@code ','} are treated as separators.</li>
     *   <li>Multiple separators are collapsed into a single dot.</li>
     *   <li>Leading/trailing separators are removed.</li>
     *   <li>Leading zeros in each segment are removed (but segment {@code "0"} stays {@code "0"}).</li>
     *   <li>If nothing meaningful remains, the result is {@code "0"}.</li>
     * </ul>
     *
     * @param version input version string; may be {@code null}
     * @return normalized version string, never {@code null}
     */
    public static String normalizeVersion(String version) {
        if (version == null) {
            return "0";
        }

        String s = version.trim();
        if (s.isEmpty()) {
            return "0";
        }

        StringBuilder out = new StringBuilder(s.length());
        boolean lastWasDot = true; // start as "dot" to avoid leading dots

        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);

            if (ch >= '0' && ch <= '9') {
                out.append(ch);
                lastWasDot = false;
                continue;
            }

            if (isSeparator(ch)) {
                if (!lastWasDot) {
                    out.append('.');
                    lastWasDot = true;
                }
            }
            // other chars are ignored
        }

        int len = out.length();
        if (len == 0) {
            return "0";
        }
        if (out.charAt(len - 1) == '.') {
            out.setLength(len - 1);
        }
        if (out.isEmpty()) {
            return "0";
        }

        List<String> segments = splitByDot(out);
        StringBuilder normalized = new StringBuilder(out.length());
        for (int i = 0; i < segments.size(); i++) {
            String seg = stripLeadingZeros(segments.get(i));
            if (seg.isEmpty()) {
                seg = "0";
            }
            if (i > 0) {
                normalized.append('.');
            }
            normalized.append(seg);
        }

        return normalized.isEmpty() ? "0" : normalized.toString();
    }

    /**
     * Compares two version strings after normalization.
     *
     * @param versionLeft  first version string; may be {@code null}
     * @param versionRight second version string; may be {@code null}
     * @return {@code 0} if equal; a negative value if left &lt; right; a positive value if left &gt; right
     */
    public static int compareVersions(String versionLeft, String versionRight) {
        String left = normalizeVersion(versionLeft);
        String right = normalizeVersion(versionRight);
        return compareNormalizedVersions(left, right);
    }

    /**
     * Parses a condition string into operator and required version.
     *
     * <p>
     * For conditions without an explicit operator, {@link ComparisonOperator#NONE} is used and
     * the entire string is treated as a required version.
     * </p>
     *
     * @param condition trimmed condition string, not blank
     * @return parsed condition
     * @throws IllegalArgumentException if the condition does not contain a version after parsing
     */
    private static ParsedCondition parseCondition(String condition) {
        ComparisonOperator operator = extractOperator(condition);
        String required = condition.substring(operator.symbolLength()).trim();
        if (required.isEmpty()) {
            throw new IllegalArgumentException("Version condition does not contain a version: " + condition);
        }
        return new ParsedCondition(operator, required);
    }

    /**
     * Extracts the comparison operator from the beginning of the condition string.
     *
     * <p>
     * Supported operators: {@code >=}, {@code >}, {@code <=}, {@code <}, {@code ==}, {@code =}.
     * If no operator is present, {@link ComparisonOperator#NONE} is returned.
     * </p>
     *
     * @param condition trimmed condition string
     * @return operator
     */
    private static ComparisonOperator extractOperator(String condition) {
        if (condition.startsWith(">=")) return ComparisonOperator.GREATER_THAN_OR_EQUAL;
        if (condition.startsWith("<=")) return ComparisonOperator.LESS_THAN_OR_EQUAL;
        if (condition.startsWith("==")) return ComparisonOperator.EQUAL;
        if (condition.startsWith(">")) return ComparisonOperator.GREATER_THAN;
        if (condition.startsWith("<")) return ComparisonOperator.LESS_THAN;
        if (condition.startsWith("=")) return ComparisonOperator.EQUAL;
        return ComparisonOperator.NONE;
    }

    /**
     * Compares two already normalized versions (dot-separated numeric segments).
     *
     * @param normalizedLeft  normalized left version (e.g. {@code "41.78.16"})
     * @param normalizedRight normalized right version (e.g. {@code "41.78.16"})
     * @return {@code 0} if equal; negative if left &lt; right; positive if left &gt; right
     */
    private static int compareNormalizedVersions(String normalizedLeft, String normalizedRight) {
        List<String> leftParts = splitByDot(new StringBuilder(normalizedLeft));
        List<String> rightParts = splitByDot(new StringBuilder(normalizedRight));

        int max = Math.max(leftParts.size(), rightParts.size());
        for (int i = 0; i < max; i++) {
            String l = (i < leftParts.size()) ? leftParts.get(i) : "0";
            String r = (i < rightParts.size()) ? rightParts.get(i) : "0";

            int cmp = compareNumericStrings(l, r);
            if (cmp != 0) {
                return cmp;
            }
        }
        return 0;
    }

    /**
     * Splits a dot-separated string builder into segments.
     *
     * <p>
     * Assumes the input contains only digits and dots and has no leading/trailing dots.
     * </p>
     *
     * @param value dot-separated digits
     * @return list of segments, never {@code null}
     */
    private static List<String> splitByDot(StringBuilder value) {
        ArrayList<String> parts = new ArrayList<>(4);
        int start = 0;
        for (int i = 0; i < value.length(); i++) {
            if (value.charAt(i) == '.') {
                parts.add(value.substring(start, i));
                start = i + 1;
            }
        }
        parts.add(value.substring(start));
        return parts;
    }

    /**
     * Compares two numeric strings as integers without parsing them into primitive types.
     *
     * @param left  numeric string (digits only)
     * @param right numeric string (digits only)
     * @return negative if left &lt; right; zero if equal; positive if left &gt; right
     */
    private static int compareNumericStrings(String left, String right) {
        String l = stripLeadingZeros(left);
        String r = stripLeadingZeros(right);

        if (l.isEmpty()) l = "0";
        if (r.isEmpty()) r = "0";

        if (l.length() != r.length()) {
            return Integer.compare(l.length(), r.length());
        }
        return l.compareTo(r);
    }

    /**
     * Strips leading zeros from a numeric string.
     *
     * @param s numeric string (may be empty)
     * @return numeric string without leading zeros; may be empty if all zeros
     */
    private static String stripLeadingZeros(String s) {
        int i = 0;
        while (i < s.length() && s.charAt(i) == '0') {
            i++;
        }
        return (i == 0) ? s : s.substring(i);
    }

    /**
     * Checks whether the provided string contains at least one digit.
     *
     * @param s input string
     * @return {@code true} if at least one digit is present
     */
    private static boolean containsAnyDigit(String s) {
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch >= '0' && ch <= '9') {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if the character should be treated as a version segment separator.
     *
     * @param ch input character
     * @return {@code true} if separator
     */
    private static boolean isSeparator(char ch) {
        return ch == '.' || ch == '-' || ch == '_' || ch == ',';
    }

    /**
     * Enumeration of supported comparison operators.
     */
    private enum ComparisonOperator {
        /**
         * No explicit operator (equality is assumed).
         */
        NONE(""),
        GREATER_THAN(">"),
        GREATER_THAN_OR_EQUAL(">="),
        LESS_THAN("<"),
        LESS_THAN_OR_EQUAL("<="),
        EQUAL("=");

        private final String symbol;

        ComparisonOperator(String symbol) {
            this.symbol = symbol;
        }

        /**
         * Returns the operator symbol length for substring slicing.
         *
         * @return operator symbol length
         */
        int symbolLength() {
            return symbol.length();
        }
    }

    /**
     * Parsed condition (operator + version string).
     *
     * @param operator        operator
     * @param requiredVersion required version string (not normalized)
     */
    private record ParsedCondition(ComparisonOperator operator, String requiredVersion) {
    }
}