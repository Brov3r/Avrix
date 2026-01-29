package com.avrix.logs;

import org.tinylog.Level;
import org.tinylog.core.ConfigurationParser;
import org.tinylog.core.LogEntry;
import org.tinylog.core.LogEntryValue;
import org.tinylog.provider.InternalLogger;
import org.tinylog.writers.AbstractFormatPatternWriter;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.util.*;

/**
 * Tinylog writer that routes formatted log output to isolated {@code stdout} and {@code stderr}
 * depending on a configured level threshold.
 */
public final class IsolatedConsoleWriter extends AbstractFormatPatternWriter {

    private static final Charset DEFAULT_CHARSET = Charset.defaultCharset();

    /**
     * Threshold that determines whether an entry goes to {@code stderr} or {@code stdout}.
     *
     * <p>
     * If {@link Level#OFF}: everything goes to stdout.
     * If {@link Level#TRACE}: everything goes to stderr.
     * Otherwise: {@code level >= errorLevel} goes to stderr, lower levels go to stdout.
     * </p>
     */
    private final Level errorLevel;

    /**
     * Print streams writing directly to OS-level stdout/stderr descriptors.
     * These streams must not be closed.
     */
    private final PrintStream outStream;
    private final PrintStream errStream;

    /**
     * Creates a new writer instance.
     *
     * @param properties tinylog writer properties, usually provided by configuration; must not be {@code null}
     */
    public IsolatedConsoleWriter(final Map<String, String> properties) {
        super(Objects.requireNonNull(properties, "properties must not be null"));

        this.errorLevel = parseErrorLevel(getStringValue("stream"));

        // Do not use System.out/System.err directly to avoid being affected by later redirections.
        this.outStream = new PrintStream(new FileOutputStream(FileDescriptor.out), true, DEFAULT_CHARSET);
        this.errStream = new PrintStream(new FileOutputStream(FileDescriptor.err), true, DEFAULT_CHARSET);
    }

    /**
     * Returns the log entry values required by this writer.
     *
     * <p>
     * This implementation requires {@link LogEntryValue#LEVEL} to decide which stream should be used.
     * </p>
     *
     * @return required values set
     */
    @Override
    public Collection<LogEntryValue> getRequiredLogEntryValues() {
        // super may return an unmodifiable collection, so we create a defensive set.
        Set<LogEntryValue> values = EnumSet.noneOf(LogEntryValue.class);
        values.addAll(super.getRequiredLogEntryValues());
        values.add(LogEntryValue.LEVEL);
        return values;
    }

    /**
     * Writes a formatted log entry to {@code stdout} or {@code stderr}.
     *
     * @param logEntry log entry to write; must not be {@code null}
     */
    @Override
    public void write(final LogEntry logEntry) {
        Objects.requireNonNull(logEntry, "logEntry must not be null");

        final String rendered = render(logEntry);
        final PrintStream target = (logEntry.getLevel().compareTo(errorLevel) < 0) ? outStream : errStream;

        // Synchronize per-stream to keep message boundaries intact under concurrency.
        synchronized (target) {
            target.print(rendered);
        }
    }

    /**
     * Flushes both output streams.
     */
    @Override
    public void flush() {
        synchronized (outStream) {
            outStream.flush();
        }
        synchronized (errStream) {
            errStream.flush();
        }
    }

    /**
     * Flushes both streams.
     *
     * <p>
     * This writer must not close the underlying process standard streams.
     * Closing {@code FileDescriptor.out/err} may break further console output in the JVM.
     * </p>
     */
    @Override
    public void close() {
        flush();
    }

    /**
     * Parses the {@code stream} property and returns the configured threshold level.
     *
     * @param streamProperty value of the {@code stream} property
     * @return threshold level used to route messages to stderr
     */
    private static Level parseErrorLevel(String streamProperty) {
        Level threshold = Level.WARN;

        if (streamProperty == null || streamProperty.isBlank()) {
            return threshold;
        }

        String value = streamProperty.trim();

        // "err@WARN"
        String[] parts = value.split("@", 2);
        if (parts.length == 2) {
            String streamName = parts[0].trim();
            if (!"err".equalsIgnoreCase(streamName)) {
                InternalLogger.log(Level.ERROR,
                        "Stream with level must be \"err\", \"" + streamName + "\" is an invalid name");
            }
            threshold = ConfigurationParser.parse(parts[1].trim(), threshold);
            return threshold;
        }

        // "out" or "err"
        if ("err".equalsIgnoreCase(value)) {
            return Level.TRACE; // everything to stderr
        }
        if ("out".equalsIgnoreCase(value)) {
            return Level.OFF; // everything to stdout
        }

        InternalLogger.log(Level.ERROR,
                "Stream must be \"out\" or \"err\", \"" + value + "\" is an invalid stream name");
        return threshold;
    }
}