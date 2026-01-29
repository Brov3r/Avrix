package com.avrix.logging;

import org.tinylog.Logger;

import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses raw Project Zomboid console output lines and routes them to TinyLog.
 * <p>
 * The parser understands several Project Zomboid log formats, extracts the log level
 * and message, normalizes the message, and forwards it to the appropriate TinyLog level.
 * </p>
 */
public final class ZomboidLogLineParser implements Consumer<String> {

    /**
     * Matches the Project Zomboid log header and extracts:
     * <ul>
     *   <li>{@code lvl} — log level token (LOG, ERROR, WARN, DEBUG, TRACE)</li>
     *   <li>{@code msg} — message part after {@code >}</li>
     * </ul>
     */
    private static final Pattern HEADER_PATTERN =
            Pattern.compile("^(?<lvl>LOG|ERROR|WARN|DEBUG|TRACE)\\s*:?\\s*(?:\\w+\\s+)?"
                    + "f:\\d+,\\s*t:\\d+(?:,\\s*st:[0-9\\s\\u00A0]+)?\\s*>\\s*(?<msg>.*)$");

    /**
     * Matches an embedded log level prefix at the beginning of a message body.
     */
    private static final Pattern PREFIX_LEVEL_PATTERN =
            Pattern.compile("^(\\[!\\]|\\[#\\]|\\[\\?\\]|\\[\\$\\]|\\[-\\])\\s*(.*)$");

    /**
     * Matches a leading numeric timestamp inside the message body.
     */
    private static final Pattern LEADING_NUMBER_TS_PATTERN =
            Pattern.compile("^\\d+\\s+(.*)$");

    /**
     * Matches excessive whitespace around {@code >} to normalize separators.
     */
    private static final Pattern SPACES_AROUND_ARROW_PATTERN =
            Pattern.compile("\\s+>\\s+");

    /**
     * Sink for lines that cannot be parsed as Project Zomboid log entries.
     */
    private final Consumer<String> fallbackSink;

    /**
     * Creates a new parser instance.
     *
     * @param fallbackSink consumer for lines that cannot be classified;
     *                     must not be {@code null}
     * @throws NullPointerException if {@code fallbackSink} is {@code null}
     */
    public ZomboidLogLineParser(Consumer<String> fallbackSink) {
        this.fallbackSink = Objects.requireNonNull(fallbackSink, "fallbackSink must not be null");
    }

    /**
     * Accepts a raw console line, parses it, and routes it to TinyLog.
     * <p>
     * {@code null} or blank lines are ignored.
     * </p>
     *
     * @param rawLine raw console output line
     */
    @Override
    public void accept(String rawLine) {
        if (rawLine == null) {
            return;
        }

        String line = rawLine.trim();
        if (line.isEmpty()) {
            return;
        }

        Parsed parsed = parse(line);
        if (parsed == null) {
            fallbackSink.accept(line);
            return;
        }

        if (parsed.message.isBlank()) {
            return;
        }

        switch (parsed.level) {
            case ERROR -> Logger.error(parsed.message);
            case WARN -> Logger.warn(parsed.message);
            case DEBUG -> Logger.debug(parsed.message);
            case TRACE -> Logger.trace(parsed.message);
            case INFO -> Logger.info(parsed.message);
        }
    }

    /**
     * Parses a single Project Zomboid log line.
     *
     * @param line non-blank line
     * @return parsed representation or {@code null} if the line format is unsupported
     */
    private static Parsed parse(String line) {
        Matcher header = HEADER_PATTERN.matcher(line);
        if (!header.matches()) {
            return null;
        }

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
     * Internal log level abstraction.
     */
    private enum Level {
        ERROR, WARN, INFO, DEBUG, TRACE;

        /**
         * Maps a header log token to an internal level.
         *
         * @param headerLevel level token from log header
         * @return corresponding {@link Level}
         */
        static Level fromHeader(String headerLevel) {
            String v = headerLevel.toUpperCase(Locale.ROOT);
            return switch (v) {
                case "ERROR" -> ERROR;
                case "WARN" -> WARN;
                case "DEBUG" -> DEBUG;
                case "TRACE" -> TRACE;
                default -> INFO; // LOG -> INFO
            };
        }

        /**
         * Maps an embedded prefix to an internal level.
         *
         * @param prefix embedded prefix (e.g. {@code [!]})
         * @return corresponding {@link Level}
         */
        static Level fromPrefix(String prefix) {
            return switch (prefix) {
                case "[!]" -> ERROR;
                case "[?]" -> WARN;
                case "[$]" -> DEBUG;
                case "[-]" -> TRACE;
                default -> INFO; // [#] and everything else -> INFO
            };
        }
    }

    /**
     * Parsed log entry representation.
     *
     * @param level   resolved log level
     * @param message normalized log message
     */
    private record Parsed(Level level, String message) {
    }
}
