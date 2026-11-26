package com.avrix.utils;

/**
 * Sanitizes untrusted text for console and user-interface presentation.
 * <p>
 * Only human-readable values may be passed through these methods. Identity values such as identifiers, versions,
 * hashes, filesystem paths, and class names must be preserved exactly and validated separately.
 */
public final class DisplayText {

    /**
     * Prevents instantiation of this stateless utility class.
     *
     * @throws UnsupportedOperationException always
     */
    private DisplayText() {
        throw new UnsupportedOperationException("DisplayText utility class cannot be instantiated");
    }

    /**
     * Sanitizes text for single-line presentation.
     * <p>
     * A {@code null} value becomes an empty string. ISO control characters, bidirectional formatting controls, and
     * Unicode line or paragraph separators are replaced with whitespace, whitespace runs are collapsed to one space,
     * and leading or trailing whitespace is removed. Other Unicode characters, including combining marks and emoji
     * sequences, are preserved.
     *
     * @param text untrusted display text, may be {@code null}
     * @return sanitized single-line text, never {@code null}
     */
    public static String sanitizeSingleLine(String text) {
        if (text == null) {
            return "";
        }

        StringBuilder sanitized = new StringBuilder(text.length());
        boolean pendingSpace = false;
        for (int offset = 0; offset < text.length(); ) {
            int codePoint = text.codePointAt(offset);
            offset += Character.charCount(codePoint);

            if (isDisplayControl(codePoint) || Character.isWhitespace(codePoint)) {
                pendingSpace = !sanitized.isEmpty();
                continue;
            }

            if (pendingSpace) {
                sanitized.append(' ');
                pendingSpace = false;
            }
            sanitized.appendCodePoint(codePoint);
        }
        return sanitized.toString();
    }

    /**
     * Sanitizes text for multiline presentation.
     * <p>
     * A {@code null} value becomes an empty string. Line feeds and tabs are preserved, and CRLF or lone CR sequences
     * are normalized to a single line feed. Other ISO control characters, bidirectional formatting controls, and
     * Unicode line or paragraph separators are replaced with a space. Other Unicode characters, including combining
     * marks and emoji sequences, are preserved. Whitespace runs are not collapsed.
     *
     * @param text untrusted display text, may be {@code null}
     * @return sanitized multiline text, never {@code null}
     */
    public static String sanitizeMultiline(String text) {
        if (text == null) {
            return "";
        }

        StringBuilder sanitized = new StringBuilder(text.length());
        for (int offset = 0; offset < text.length(); ) {
            int codePoint = text.codePointAt(offset);
            offset += Character.charCount(codePoint);

            if (codePoint == '\r') {
                if (offset < text.length() && text.charAt(offset) == '\n') {
                    offset++;
                }
                sanitized.append('\n');
            } else if (codePoint == '\n' || codePoint == '\t') {
                sanitized.appendCodePoint(codePoint);
            } else if (isDisplayControl(codePoint)) {
                sanitized.append(' ');
            } else {
                sanitized.appendCodePoint(codePoint);
            }
        }
        return sanitized.toString();
    }

    private static boolean isDisplayControl(int codePoint) {
        if (Character.isISOControl(codePoint)) {
            return true;
        }
        if (codePoint == '\u2028' || codePoint == '\u2029') {
            return true;
        }
        return codePoint == '\u061C'
                || codePoint == '\u200E'
                || codePoint == '\u200F'
                || (codePoint >= '\u202A' && codePoint <= '\u202E')
                || (codePoint >= '\u2066' && codePoint <= '\u2069');
    }
}
