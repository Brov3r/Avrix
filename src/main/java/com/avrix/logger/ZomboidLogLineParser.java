package com.avrix.logger;

import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses Project Zomboid log lines and routes them to appropriate log level consumers.
 * <p>
 * It identifies the log level from the line header or message prefix and strips formatting
 * before passing the clean message to the corresponding sink.
 */
public final class ZomboidLogLineParser implements Consumer<String> {

    /**
     * Regex pattern to match and extract components from a Zomboid log header.
     */
    private static final Pattern HEADER_PATTERN =
            Pattern.compile("^[ \t]*(?<lvl>LOG|ERROR|WARN|DEBUG|TRACE)\\s*:?\\s*(?:\\S+\\s+)?(?:f:\\d+(?:,[^>]*)?\\s*)?(?:st:[^>]+\\s*)?(?:at\\s+\\S+\\s*)?>[ \t]?(?:\\d+\\s+)?(?<msg>.*)$");
    
    /**
     * Regex pattern to normalize spaces around the '>' arrow in log messages.
     */
    private static final Pattern SPACES_AROUND_ARROW_PATTERN =
            Pattern.compile("\\s+>\\s?");

    private final Consumer<String> errorSink;
    private final Consumer<String> warnSink;
    private final Consumer<String> infoSink;
    private final Consumer<String> debugSink;
    private final Consumer<String> traceSink;
    private final Consumer<String> fallbackSink;

    /**
     * Creates a new parser with the specified log level sinks.
     *
     * @param errorSink    consumer for ERROR level messages
     * @param warnSink     consumer for WARN level messages
     * @param infoSink     consumer for INFO level messages
     * @param debugSink    consumer for DEBUG level messages
     * @param traceSink    consumer for TRACE level messages
     * @param fallbackSink consumer for unparsed or unrecognized messages
     */
    public ZomboidLogLineParser(Consumer<String> errorSink,
                                Consumer<String> warnSink,
                                Consumer<String> infoSink,
                                Consumer<String> debugSink,
                                Consumer<String> traceSink,
                                Consumer<String> fallbackSink) {
        this.errorSink = Objects.requireNonNull(errorSink, "errorSink must not be null");
        this.warnSink = Objects.requireNonNull(warnSink, "warnSink must not be null");
        this.infoSink = Objects.requireNonNull(infoSink, "infoSink must not be null");
        this.debugSink = Objects.requireNonNull(debugSink, "debugSink must not be null");
        this.traceSink = Objects.requireNonNull(traceSink, "traceSink must not be null");
        this.fallbackSink = Objects.requireNonNull(fallbackSink, "fallbackSink must not be null");
    }

    /**
     * Parses a raw log line and routes it to the appropriate sink.
     *
     * @param rawLine the raw log line to process
     */
    @Override
    public void accept(String rawLine) {
        if (rawLine == null) return;

        String line = rawLine.stripTrailing();
        if (line.isEmpty()) return;

        Parsed parsed = parse(line);
        if (parsed == null) {
            fallbackSink.accept(line);
            return;
        }

        if (parsed.message().isBlank()) return;

        switch (parsed.level()) {
            case ERROR -> errorSink.accept(parsed.message());
            case WARN -> warnSink.accept(parsed.message());
            case DEBUG -> debugSink.accept(parsed.message());
            case TRACE -> traceSink.accept(parsed.message());
            case INFO -> infoSink.accept(parsed.message());
        }
    }

    /**
     * Extracts the log level and message from a formatted log line.
     *
     * @param line the log line to parse
     * @return a {@link Parsed} record containing the level and message, or {@code null} if unrecognized
     */
    private static Parsed parse(String line) {
        Matcher header = HEADER_PATTERN.matcher(line);
        if (!header.matches()) return null;

        Level level = Level.fromHeader(header.group("lvl"));
        String message = header.group("msg");

        String checkMsg = message.stripLeading();
        if (checkMsg.length() >= 3 && checkMsg.charAt(0) == '[' && checkMsg.charAt(2) == ']') {
            Level prefixLevel = Level.fromPrefix(checkMsg.charAt(1));
            if (prefixLevel != null) {
                level = prefixLevel;
                int closeBracketIdx = checkMsg.indexOf(']');
                message = checkMsg.substring(closeBracketIdx + 1).stripLeading();
            }
        }

        if (message.contains(">")) {
            message = SPACES_AROUND_ARROW_PATTERN.matcher(message).replaceAll(" > ");
        }

        return new Parsed(level, message);
    }

    /**
     * Represents the supported log levels and provides parsing utilities.
     */
    private enum Level {
        ERROR, WARN, INFO, DEBUG, TRACE;

        /**
         * Resolves a log level from a header token.
         *
         * @param token the header token
         * @return the corresponding log level
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
         * Resolves a log level from a message prefix character.
         *
         * @param prefix the prefix character
         * @return the corresponding log level, or {@code null} if unrecognized
         */
        static Level fromPrefix(char prefix) {
            return switch (prefix) {
                case '!' -> ERROR;
                case '?' -> WARN;
                case '$' -> DEBUG;
                case '-' -> TRACE;
                case '#' -> INFO;
                default -> null;
            };
        }
    }

    /**
     * Holds the result of parsing a log line.
     *
     * @param level   the detected log level
     * @param message the extracted log message
     */
    private record Parsed(Level level, String message) {
    }
}