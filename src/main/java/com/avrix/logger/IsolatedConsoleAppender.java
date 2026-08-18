package com.avrix.logger;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.encoder.Encoder;
import ch.qos.logback.core.encoder.LayoutWrappingEncoder;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.concurrent.locks.ReentrantLock;

/**
 * High-performance console appender that writes directly to low-level standard OS file descriptors.
 * <p>
 * Bypasses {@link System#out} and {@link System#err} redirections that might be installed
 * by the target game runtime or third-party mods, ensuring uninterrupted loader log output.
 *
 * @apiNote Thread-safe via internal {@link ReentrantLock} synchronizing writes to output streams.
 */
public final class IsolatedConsoleAppender extends AppenderBase<ILoggingEvent> {

    private final OutputStream outStream;
    private final OutputStream errStream;
    private final ReentrantLock lock = new ReentrantLock();

    private Encoder<ILoggingEvent> encoder;
    private Level errorLevel = Level.WARN;

    /**
     * Constructs a new {@link IsolatedConsoleAppender} bound to raw stdout/stderr file descriptors.
     */
    public IsolatedConsoleAppender() {
        this.outStream = new FileOutputStream(FileDescriptor.out);
        this.errStream = new FileOutputStream(FileDescriptor.err);
    }

    /**
     * Retrieves the configured log event encoder.
     *
     * @return the active {@link Encoder} instance, or {@code null} if none is set
     */
    public Encoder<ILoggingEvent> getEncoder() {
        return encoder;
    }

    /**
     * Sets the encoder used to format and serialize log events into byte arrays.
     *
     * @param encoder the log event encoder to attach
     */
    public void setEncoder(Encoder<ILoggingEvent> encoder) {
        this.encoder = encoder;
    }

    /**
     * Retrieves the string representation of the current error routing threshold.
     *
     * @return string name of the threshold {@link Level}
     */
    public String getErrorLevel() {
        return errorLevel.toString();
    }

    /**
     * Sets the threshold level at and above which messages will be routed to standard error.
     *
     * @param levelStr the log level name (e.g., {@code "WARN"}, {@code "ERROR"}), defaults to {@code WARN} if null/blank
     */
    public void setErrorLevel(String levelStr) {
        if (levelStr == null || levelStr.isBlank()) {
            this.errorLevel = Level.WARN;
        } else {
            this.errorLevel = Level.toLevel(levelStr, Level.WARN);
        }
    }

    /**
     * Appends a single logging event by serializing it through the encoder and writing to OS descriptors.
     *
     * @param event the logging event to process
     * @implNote Directs output to {@link FileDescriptor#err} if the event level is greater than or
     * equal to {@link #errorLevel}; otherwise routes to {@link FileDescriptor#out}.
     */
    @Override
    protected void append(ILoggingEvent event) {
        if (event == null || !isStarted() || encoder == null) {
            return;
        }

        try {
            byte[] bytes = encoder.encode(event);
            if (bytes == null || bytes.length == 0) {
                return;
            }

            OutputStream targetStream = (event.getLevel().toInt() >= errorLevel.toInt()) ? errStream : outStream;

            lock.lock();
            try {
                targetStream.write(bytes);
                targetStream.flush();
            } finally {
                lock.unlock();
            }
        } catch (IOException e) {
            addError("Failed to write log event directly to isolated console descriptors", e);
        }
    }

    /**
     * Starts the appender, validates encoder configuration, and configures console character encoding.
     *
     * @implNote Detects the active console encoding via {@link System#console()} with fallback to {@link System#out}.
     */
    @Override
    public void start() {
        if (encoder == null) {
            addError("No encoder configured for IsolatedConsoleAppender. Appender will not start.");
            return;
        }

        if (encoder instanceof LayoutWrappingEncoder<?> layoutEncoder) {
            Charset consoleCharset = (System.console() != null)
                    ? System.console().charset()
                    : System.out.charset();
            layoutEncoder.setCharset(consoleCharset);
        }

        if (!encoder.isStarted()) {
            encoder.start();
        }

        byte[] headerBytes = encoder.headerBytes();
        if (headerBytes != null && headerBytes.length > 0) {
            writeBytesDirectly(outStream, headerBytes);
        }

        super.start();
    }

    /**
     * Stops the appender, flushes any remaining buffered output, and releases encoder resources.
     */
    @Override
    public void stop() {
        if (!isStarted()) {
            return;
        }

        lock.lock();
        try {
            if (encoder != null) {
                byte[] footerBytes = encoder.footerBytes();
                if (footerBytes != null && footerBytes.length > 0) {
                    writeBytesDirectly(outStream, footerBytes);
                }
                if (encoder.isStarted()) {
                    encoder.stop();
                }
            }

            outStream.flush();
            errStream.flush();
        } catch (IOException e) {
            addError("Failed to flush output streams during appender shutdown", e);
        } finally {
            lock.unlock();
        }

        super.stop();
    }

    /**
     * Writes raw metadata byte arrays directly to the given stream under lock.
     *
     * @param stream the target output stream
     * @param bytes  the byte array to write
     */
    private void writeBytesDirectly(OutputStream stream, byte[] bytes) {
        lock.lock();
        try {
            stream.write(bytes);
            stream.flush();
        } catch (IOException e) {
            addError("Failed to write metadata bytes to output stream", e);
        } finally {
            lock.unlock();
        }
    }
}