package com.avrix.provider.zomboid.approval;

/**
 * Bounds unapproved metadata presentation and the approval subprocess protocol.
 */
public final class ApprovalLimits {

    /** Maximum archive or decision count in one batch. */
    public static final int MAX_ENTRIES = 4096;
    /** Maximum length of a human-readable metadata field, including its truncation marker. */
    public static final int MAX_DISPLAY_CHARACTERS = 1024;
    /** Maximum length of an identifier or class name; identity fields are never truncated. */
    public static final int MAX_IDENTIFIER_CHARACTERS = 512;
    /** Maximum decoded length of a protocol field, including paths and joined mixin names. */
    public static final int MAX_FIELD_CHARACTERS = 32768;
    /** Maximum declared mixin count per archive. */
    public static final int MAX_MIXINS = 1024;
    /** Maximum number of characters in a protocol line. */
    public static final int MAX_LINE_CHARACTERS = 256 * 1024;
    /** Maximum number of characters in a complete request. */
    public static final int MAX_REQUEST_CHARACTERS = 8 * 1024 * 1024;
    /** Maximum number of characters in a complete response. */
    public static final int MAX_RESPONSE_CHARACTERS = 1024 * 1024;
    /** Maximum uncompressed class size inspected before approval. */
    public static final int MAX_CLASS_BYTES = 4 * 1024 * 1024;

    private ApprovalLimits() {
        throw new UnsupportedOperationException("ApprovalLimits utility class cannot be instantiated");
    }

    /**
     * Truncates presentation text without splitting a UTF-16 surrogate pair.
     *
     * @param text non-null display text
     * @return bounded text with an ellipsis when truncated
     */
    public static String displayText(String text) {
        if (text.length() <= MAX_DISPLAY_CHARACTERS) {
            return text;
        }
        int end = MAX_DISPLAY_CHARACTERS - 1;
        if (Character.isHighSurrogate(text.charAt(end - 1))) {
            end--;
        }
        return text.substring(0, end) + "…";
    }

    /**
     * Requires bounded text without changing identity or filesystem values.
     *
     * @param text non-null value
     * @param limit maximum character count
     * @param field diagnostic field name
     * @return the unchanged value
     * @throws IllegalArgumentException if the value exceeds the limit
     */
    public static String requireLength(String text, int limit, String field) {
        if (text.length() > limit) {
            throw new IllegalArgumentException(field + " exceeds approval limit of " + limit + " characters");
        }
        return text;
    }
}
