package com.avrix.logs;

import org.tinylog.Logger;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An OutputStream implementation that reads byte arrays, interprets them as lines of text,
 * and processes each line based on specific rules for logging or consumption.
 */
public class LineReadingOutputStream extends OutputStream {

    private static final byte CR = '\r'; // Carriage Return
    private static final byte LF = '\n'; // Line Feed

    private final Consumer<String> consumer; // Consumer for processed lines
    private final StringBuilder stringBuilder = new StringBuilder(); // Accumulates bytes into lines
    private boolean lastCR = false; // Tracks the last encountered Carriage Return

    /**
     * Constructs a LineReadingOutputStream with the specified consumer.
     *
     * @param consumer The consumer to accept processed lines.
     * @throws NullPointerException if the consumer is null.
     */
    public LineReadingOutputStream(final Consumer<String> consumer) {
        this.consumer = Objects.requireNonNull(consumer, "[!] Consumer must not be null");
    }

    /**
     * Writes a single byte to this output stream.
     *
     * @param b The byte to be written.
     * @throws IOException if an I/O error occurs.
     */
    @Override
    public void write(final int b) throws IOException {
        write(new byte[]{(byte) b});
    }

    /**
     * Writes a portion of a byte array to this output stream.
     *
     * @param b     The data.
     * @param start The start offset in the data.
     * @param len   The number of bytes to write.
     * @throws IllegalArgumentException if the start or len is negative, or end exceeds the array length.
     */
    @Override
    public void write(final byte[] b, int start, final int len) {
        if (b == null) {
            throw new NullPointerException("[!] Byte array must not be null");
        }
        if (len < 0) {
            throw new IllegalArgumentException("[!] Length must not be negative");
        }
        final int end = start + len;
        if ((start < 0) || (start > b.length) || (end < 0) || (end > b.length)) {
            throw new IndexOutOfBoundsException("[!] Invalid start or end index");
        }

        // Handle Carriage Return and Line Feed scenarios
        if (this.lastCR && start < end && b[start] == LF) {
            start++;
            this.lastCR = false;
        } else if (start < end) {
            this.lastCR = b[end - 1] == CR;
        }

        int base = start;
        for (int i = start; i < end; i++) {
            if (b[i] == LF || b[i] == CR) {
                final String chunk = asString(b, base, i);
                this.stringBuilder.append(chunk);
                consume();
            }
            if (b[i] == LF) {
                base = i + 1;
            } else if (b[i] == CR) {
                if (i < end - 1 && b[i + 1] == LF) {
                    base = i + 2;
                    i++;
                } else {
                    base = i + 1;
                }
            }
        }
        final String chunk = asString(b, base, end);
        this.stringBuilder.append(chunk);
    }

    /**
     * Closes this output stream and flushes any accumulated data if present.
     */
    @Override
    public void close() {
        if (!this.stringBuilder.isEmpty()) {
            consume();
        }
    }

    /**
     * Converts a byte array slice to a UTF-8 encoded string.
     *
     * @param b     The byte array.
     * @param start The start index.
     * @param end   The end index (exclusive).
     * @return The decoded string.
     * @throws IllegalArgumentException if start is greater than end.
     */
    private static String asString(final byte[] b, final int start, final int end) {
        if (start > end) {
            throw new IllegalArgumentException("[!] Start index must be <= end index");
        }
        if (start == end) {
            return "";
        }
        final byte[] copy = Arrays.copyOfRange(b, start, end);
        return new String(copy, StandardCharsets.UTF_8);
    }

    /**
     * Processes the accumulated string builder content.
     * Parses and logs or consumes each line based on specific rules.
     */
    private void consume() {
        String text = this.stringBuilder.toString();
        this.stringBuilder.setLength(0);

        if (text.isEmpty()) return;

        // Regular expression to capture the first word and message after '>'
        Pattern pattern1 = Pattern.compile("^(\\w+).*?>.*?>\\s(.*)$");
        Matcher matcher1 = pattern1.matcher(text);

        // Regular expression to capture the first word and message after '>'
        Pattern pattern2 = Pattern.compile("^(\\w+).*?>\\s(.*)$");
        Matcher matcher2 = pattern2.matcher(text);

        Matcher matcherToUse = null;
        if (matcher1.find()) {
            matcherToUse = matcher1;
        } else if (matcher2.find()) {
            matcherToUse = matcher2;
        }

        if (matcherToUse != null) {
            String firstWord = matcherToUse.group(1);
            String message = matcherToUse.group(2);

            message = message.replaceAll("\\[.*?\\]\\s*>\\s*", "").trim();
            message = message.replaceAll("^\\d*\\s", "").trim();
            message = message.replaceAll("\\s*>\\s*", " > ").trim();
            message = capitalizeFirstLetter(message);

            if (message.isEmpty()) return;

            Pattern specialPattern = Pattern.compile("^\\[(.)\\]\\s*(.*)$");
            Matcher specialMatcher = specialPattern.matcher(message);

            if (specialMatcher.find()) {
                char specialChar = specialMatcher.group(1).charAt(0);
                String messageFormated = specialMatcher.group(2).trim();

                switch (specialChar) {
                    case '!':
                        Logger.error(messageFormated);
                        break;
                    case '?':
                        Logger.warn(messageFormated);
                        break;
                    default:
                        Logger.info(messageFormated);
                        break;
                }
                return;
            }

            switch (firstWord) {
                case "DEBUG":
                    Logger.debug(message);
                    break;
                case "WARN":
                    Logger.warn(message);
                    break;
                case "ERROR":
                    Logger.error(message);
                    break;
                case "TRACE":
                    Logger.trace(message);
                    break;
                default:
                    if (message.startsWith("DEBUG:")) {
                        Logger.debug(capitalizeFirstLetter(message.substring(6).trim()));
                    } else if (message.startsWith("WARN:")) {
                        Logger.warn(capitalizeFirstLetter(message.substring(5).trim()));
                    } else if (message.startsWith("ERROR:")) {
                        Logger.error(capitalizeFirstLetter(message.substring(6).trim()));
                    } else if (message.startsWith("TRACE:")) {
                        Logger.trace(capitalizeFirstLetter(message.substring(6).trim()));
                    } else {
                        this.consumer.accept(message);
                    }
                    break;
            }
        } else {
            // Handle special cases based on first characters in square brackets
            Pattern specialPattern = Pattern.compile("^\\[(.)\\]\\s*(.*)$");
            Matcher specialMatcher = specialPattern.matcher(text);
            if (specialMatcher.find()) {
                char specialChar = specialMatcher.group(1).charAt(0);
                String message = specialMatcher.group(2).trim();

                switch (specialChar) {
                    case '!':
                        Logger.error(message);
                        break;
                    case '?':
                        Logger.warn(message);
                        break;
                    default:
                        Logger.info(message);
                        break;
                }
                return;
            }

            this.consumer.accept(capitalizeFirstLetter(text));
        }
    }

    /**
     * Capitalizes the first letter of a string.
     *
     * @param message The input string.
     * @return The input string with the first letter capitalized.
     */
    private String capitalizeFirstLetter(String message) {
        if (message == null || message.isEmpty()) {
            return message;
        }
        return message.substring(0, 1).toUpperCase() + message.substring(1);
    }
}