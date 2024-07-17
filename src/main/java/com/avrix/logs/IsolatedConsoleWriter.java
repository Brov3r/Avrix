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
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * Console writer for Tinylog that redirects logging to isolated streams.
 * This writer is based on {@link org.tinylog.writers.ConsoleWriter} but redirects output
 * to custom streams, allowing isolation from {@code System.out} and {@code System.err}.
 * Logs of different levels are written to either the standard output stream or error stream
 * based on configured error levels.
 */
public class IsolatedConsoleWriter extends AbstractFormatPatternWriter {

    private final Level errorLevel;
    private final PrintStream outStream, errStream;

    /**
     * Constructs a new IsolatedConsoleWriter with default properties.
     */
    public IsolatedConsoleWriter() {
        this(Collections.emptyMap());
    }

    /**
     * Constructs a new IsolatedConsoleWriter with custom properties.
     *
     * @param properties The properties map for custom configuration.
     */
    public IsolatedConsoleWriter(final Map<String, String> properties) {
        super(properties);

        Level levelStream = Level.WARN;

        // Check stream property
        String stream = getStringValue("stream");
        if (stream != null) {
            // Check whether we have the err@LEVEL syntax
            String[] streams = stream.split("@", 2);
            if (streams.length == 2) {
                levelStream = ConfigurationParser.parse(streams[1], levelStream);
                if (!streams[0].equals("err")) {
                    InternalLogger.log(Level.ERROR, "[!] Stream with level must be \"err\", \"" + streams[0] + "\" is an invalid name");
                }
                stream = null;
            }
        }

        if (stream == null) {
            errorLevel = levelStream;
        } else if ("err".equalsIgnoreCase(stream)) {
            errorLevel = Level.TRACE;
        } else if ("out".equalsIgnoreCase(stream)) {
            errorLevel = Level.OFF;
        } else {
            InternalLogger.log(Level.ERROR, "[!] Stream must be \"out\" or \"err\", \"" + stream + "\" is an invalid stream name");
            errorLevel = levelStream;
        }

        outStream = new PrintStream(new FileOutputStream(FileDescriptor.out), true);
        errStream = new PrintStream(new FileOutputStream(FileDescriptor.err), true);
    }

    /**
     * Returns the collection of log entry values required by this writer.
     *
     * @return A collection of log entry values including {@link LogEntryValue#LEVEL}.
     */
    @Override
    public Collection<LogEntryValue> getRequiredLogEntryValues() {
        Collection<LogEntryValue> logEntryValues = super.getRequiredLogEntryValues();
        logEntryValues.add(LogEntryValue.LEVEL);
        return logEntryValues;
    }

    /**
     * Writes the log entry to the appropriate output stream based on the log entry's level.
     *
     * @param logEntry The log entry to be written.
     */
    @Override
    public void write(final LogEntry logEntry) {
        if (logEntry.getLevel().ordinal() < errorLevel.ordinal()) {
            outStream.print(render(logEntry));
        } else {
            errStream.print(render(logEntry));
        }
    }

    /**
     * Flushes both the standard output and error streams.
     */
    @Override
    public void flush() {
        outStream.flush();
        errStream.flush();
    }

    /**
     * Closes both the standard output and error streams.
     */
    @Override
    public void close() {
        outStream.close();
        errStream.close();
    }
}