package com.avrix.logger;

import org.tinylog.Logger;

import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses raw Project Zomboid console output lines and routes them to TinyLog.
 * Strips engine-specific metadata (log level, category, frame counters, source locations)
 * and forwards normalized messages to the appropriate logging level.
 * Unrecognized lines are delegated to a configurable fallback consumer.
 */
public final class ZomboidLogLineParser implements Consumer<String> {

    /**
     * Matches the primary PZ log header, extracting the level token and message body.
     */
    private static final Pattern HEADER_PATTERN =
            Pattern.compile("^(?<lvl>LOG|ERROR|WARN|DEBUG|TRACE)\\s*:?\\s*(?:\\S+\\s+)?(?:f:\\d+(?:,[^>]*)?\\s*)?(?:st:[^>]+\\s*)?(?:at\\s+\\S+\\s*)?>\\s*(?<msg>.*)$");
    /**
     * Detects embedded level prefixes (e.g., {@code [!]}, {@code [?]}) within message bodies.
     */
    private static final Pattern PREFIX_LEVEL_PATTERN =
            Pattern.compile("^(\\[!\\]|\\[#\\]|\\[\\?\\]|\\[\\$\\]|\\[-\\])\\s*(.*)$");

    /**
     * Removes leading numeric timestamps that occasionally appear in PZ log messages.
     */
    private static final Pattern LEADING_NUMBER_TS_PATTERN =
            Pattern.compile("^\\d+\\s+(.*)$");

    /**
     * Normalizes excessive whitespace around the {@code >} separator.
     */
    private static final Pattern SPACES_AROUND_ARROW_PATTERN =
            Pattern.compile("\\s+>\\s+");

    /**
     * Consumer for lines that do not match the expected PZ log format.
     */
    private final Consumer<String> fallbackSink;

    /**
     * Creates a parser instance with a designated fallback handler.
     *
     * @param fallbackSink consumer for unrecognized or malformed lines; must not be {@code null}
     * @throws NullPointerException if {@code fallbackSink} is {@code null}
     */
    public ZomboidLogLineParser(Consumer<String> fallbackSink) {
        this.fallbackSink = Objects.requireNonNull(fallbackSink, "fallbackSink must not be null");
    }

    /**
     * Processes a single raw console line, extracts structured log data, and routes it to TinyLog.
     * Blank lines are silently ignored. Unparseable content is forwarded to the fallback sink.
     *
     * @param rawLine the raw console output line to process
     */
    @Override
    public void accept(String rawLine) {
        if (rawLine == null) return;
        String line = rawLine.trim();
        if (line.isEmpty()) return;

        Parsed parsed = parse(line);
        if (parsed == null) {
            fallbackSink.accept(line);
            return;
        }
        if (parsed.message.isBlank()) return;

        switch (parsed.level) {
            case ERROR -> Logger.error(parsed.message);
            case WARN -> Logger.warn(parsed.message);
            case DEBUG -> Logger.debug(parsed.message);
            case TRACE -> Logger.trace(parsed.message);
            case INFO -> Logger.info(parsed.message);
        }
    }

    /**
     * Attempts to parse a trimmed log line into its level and message components.
     * Applies sequential normalization: header extraction, prefix resolution, and whitespace cleanup.
     *
     * @param line a non-blank, trimmed console line
     * @return parsed log entry, or {@code null} if the format is unsupported
     */
    private static Parsed parse(String line) {
        Matcher header = HEADER_PATTERN.matcher(line);
        if (!header.matches()) return null;

        Level level = Level.fromHeader(header.group("lvl"));
        String message = header.group("msg").trim();

        Matcher prefix = PREFIX_LEVEL_PATTERN.matcher(message);
        if (prefix.matches()) {
            level = Level.fromPrefix(prefix.group(1));
            message = prefix.group(2).trim();
        }

        message = SPACES_AROUND_ARROW_PATTERN.matcher(message).replaceAll(" > ");

        Matcher ts = LEADING_NUMBER_TS_PATTERN.matcher(message);
        if (ts.matches()) {
            message = ts.group(1).trim();
        }

        return new Parsed(level, message);
    }

    /**
     * Internal representation of supported logging levels.
     * Maps engine-specific tokens and prefixes to standard TinyLog levels.
     */
    private enum Level {
        ERROR, WARN, INFO, DEBUG, TRACE;

        /**
         * Resolves a header token extracted from the log line to a standard level.
         *
         * @param token raw level token (e.g., {@code LOG}, {@code ERROR})
         * @return corresponding internal level; defaults to {@link #INFO}
         */
        static Level fromHeader(String token) {
            return switch (token.toUpperCase(Locale.ROOT)) {
                case "ERROR" -> ERROR;
                case "WARN" -> WARN;
                case "DEBUG" -> DEBUG;
                case "TRACE" -> TRACE;
                default -> INFO;
            };
        }

        /**
         * Resolves an embedded message prefix to a standard level.
         *
         * @param prefix raw prefix string (e.g., {@code [!]}, {@code [?]})
         * @return corresponding internal level; defaults to {@link #INFO}
         */
        static Level fromPrefix(String prefix) {
            return switch (prefix) {
                case "[!]" -> ERROR;
                case "[?]" -> WARN;
                case "[$]" -> DEBUG;
                case "[-]" -> TRACE;
                default -> INFO;
            };
        }
    }

    /**
     * Holds the result of a successful log line parsing operation.
     *
     * @param level   resolved logging level
     * @param message normalized message content ready for output
     */
    private record Parsed(Level level, String message) {
    }
}