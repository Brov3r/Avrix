package com.avrix.provider.zomboid.approval.ui;

import com.avrix.provider.zomboid.approval.ApprovalMessage;
import com.avrix.provider.zomboid.approval.ApprovalMessages;

import java.awt.Font;
import java.awt.FontMetrics;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Formats values and builds HTML text for the approval window.
 */
public final class ApprovalFormatting {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());

    private ApprovalFormatting() {
        throw new UnsupportedOperationException("ApprovalFormatting utility class cannot be instantiated");
    }

    /**
     * Formats a byte count for display.
     *
     * @param bytes byte count
     * @return compact human-readable size
     */
    public static String formatSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        if (bytes < 1024L * 1024L) {
            return String.format("%.1f KiB", bytes / 1024.0);
        }
        return String.format("%.1f MiB", bytes / (1024.0 * 1024.0));
    }

    /**
     * Formats a last-modified timestamp.
     *
     * @param epochMillis epoch milliseconds, or a non-positive value when unavailable
     * @param messages    localized messages
     * @return formatted date or a localized unknown marker
     */
    public static String formatDate(long epochMillis, ApprovalMessages messages) {
        return epochMillis <= 0L
                ? messages.get(ApprovalMessage.DATE_UNKNOWN)
                : DATE_FORMAT.format(Instant.ofEpochMilli(epochMillis));
    }

    /**
     * Replaces a blank value with an em dash.
     *
     * @param value source value
     * @return source value or an em dash
     */
    public static String blankToDash(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }

    /**
     * Escapes text for embedding in HTML.
     *
     * @param value source value
     * @return escaped text
     */
    public static String escapeHtml(String value) {
        return value == null ? "" : value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    /**
     * Wraps text in a fixed-width HTML body so {@link javax.swing.JLabel} can render multiple lines.
     *
     * @param value source text
     * @param width available width in pixels
     * @return HTML text
     */
    public static String htmlText(String value, int width) {
        return "<html><body style='width:" + Math.max(240, width) + "px; margin:0'>"
                + escapeHtml(value)
                + "</body></html>";
    }

    /**
     * Wraps text to the available pixel width.
     *
     * @param text    source text
     * @param metrics font metrics
     * @param width   available width
     * @return wrapped lines
     */
    public static List<String> wrapLines(String text, FontMetrics metrics, int width) {
        int maxWidth = Math.max(80, width - 8);
        List<String> result = new ArrayList<>();
        for (String paragraph : text.split("\\R", -1)) {
            if (paragraph.isEmpty()) {
                result.add("");
                continue;
            }

            StringBuilder current = new StringBuilder();
            for (String word : paragraph.split(" ")) {
                String candidate = current.length() == 0 ? word : current + " " + word;
                if (metrics.stringWidth(candidate) <= maxWidth) {
                    current.setLength(0);
                    current.append(candidate);
                    continue;
                }

                if (current.length() > 0) {
                    result.add(current.toString());
                    current.setLength(0);
                }

                String remaining = word;
                while (metrics.stringWidth(remaining) > maxWidth && remaining.length() > 1) {
                    int split = remaining.length();
                    while (split > 1 && metrics.stringWidth(remaining.substring(0, split)) > maxWidth) {
                        split--;
                    }
                    result.add(remaining.substring(0, split));
                    remaining = remaining.substring(split);
                }
                current.append(remaining);
            }

            if (current.length() > 0) {
                result.add(current.toString());
            }
        }
        return result;
    }

    /**
     * Truncates text with an ellipsis when it exceeds the available width.
     *
     * @param text     source text
     * @param metrics  font metrics
     * @param maxWidth maximum width
     * @return truncated text
     */
    public static String ellipsize(String text, FontMetrics metrics, int maxWidth) {
        if (metrics.stringWidth(text) <= maxWidth) {
            return text;
        }
        String suffix = "…";
        int end = text.length();
        while (end > 0 && metrics.stringWidth(text.substring(0, end) + suffix) > maxWidth) {
            end--;
        }
        return text.substring(0, end) + suffix;
    }
}
