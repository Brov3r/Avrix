package com.avrix.logging;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * {@link OutputStream} that collects written bytes and emits complete text lines to a consumer.
 */
public final class LineReadingOutputStream extends OutputStream {

    private static final byte CR = '\r';
    private static final byte LF = '\n';

    private final Object lock = new Object();

    private final Consumer<String> lineConsumer;
    private final Charset charset;

    /**
     * Accumulates bytes of the current (not yet terminated) line.
     */
    private final ByteArrayOutputStream lineBuffer = new ByteArrayOutputStream(256);

    /**
     * Remembers whether the last processed byte was {@code '\r'} to support CRLF.
     */
    private boolean lastWasCR;

    /**
     * Creates a stream that emits lines to {@code lineConsumer} using the given charset.
     *
     * @param lineConsumer callback invoked for each complete line; must not be {@code null}
     * @param charset      charset used to decode line bytes; must not be {@code null}
     */
    public LineReadingOutputStream(Consumer<String> lineConsumer, Charset charset) {
        this.lineConsumer = Objects.requireNonNull(lineConsumer, "lineConsumer must not be null");
        this.charset = Objects.requireNonNull(charset, "charset must not be null");
    }

    /**
     * Creates a stream that emits lines to {@code lineConsumer} using UTF-8.
     *
     * @param lineConsumer callback invoked for each complete line; must not be {@code null}
     */
    public LineReadingOutputStream(Consumer<String> lineConsumer) {
        this(lineConsumer, StandardCharsets.UTF_8);
    }

    /**
     * Writes a single byte to this stream.
     *
     * <p>
     * If the byte is a line separator, the current line (if any) is decoded and emitted.
     * Otherwise the byte is appended to the current line buffer.
     * </p>
     *
     * @param b byte to write (only low 8 bits are used)
     */
    @Override
    public void write(int b) {
        byte value = (byte) b;
        writeByte(value);
    }

    /**
     * Writes a byte array slice to this stream.
     *
     * <p>
     * The slice is scanned for line separators. Each complete line is emitted in order.
     * </p>
     *
     * @param bytes  source array; must not be {@code null}
     * @param offset start offset in array
     * @param length number of bytes to write
     * @throws NullPointerException      if {@code bytes} is {@code null}
     * @throws IndexOutOfBoundsException if {@code offset/length} are invalid
     */
    @Override
    public void write(byte[] bytes, int offset, int length) {
        Objects.requireNonNull(bytes, "bytes must not be null");
        if (offset < 0 || length < 0 || offset + length > bytes.length) {
            throw new IndexOutOfBoundsException("Invalid offset/length: offset=" + offset + ", length=" + length);
        }

        synchronized (lock) {
            int end = offset + length;
            for (int i = offset; i < end; i++) {
                writeByteLocked(bytes[i]);
            }
        }
    }

    /**
     * Flushes the current buffered line (if any) without requiring a newline.
     *
     * <p>
     * This behavior is useful when the upstream code calls {@link PrintStream#flush()}.
     * </p>
     */
    @Override
    public void flush() {
        synchronized (lock) {
            emitLineLocked();
        }
    }

    /**
     * Emits the last buffered line (if any) and releases internal resources.
     *
     * <p>
     * This method does not close the provided consumer; it only ensures that all buffered content
     * is delivered.
     * </p>
     */
    @Override
    public void close() throws IOException {
        synchronized (lock) {
            emitLineLocked();
        }
    }

    /**
     * Processes a single byte with synchronization.
     *
     * @param value byte to process
     */
    private void writeByte(byte value) {
        synchronized (lock) {
            writeByteLocked(value);
        }
    }

    /**
     * Processes a single byte and updates the line buffer and CRLF state.
     *
     * <p>
     * The method assumes the caller holds {@link #lock}.
     * </p>
     *
     * @param value byte to process
     */
    private void writeByteLocked(byte value) {
        if (value == CR) {
            emitLineLocked();
            lastWasCR = true;
            return;
        }

        if (value == LF) {
            // If previous byte was CR, this is the LF part of CRLF: ignore.
            if (!lastWasCR) {
                emitLineLocked();
            }
            lastWasCR = false;
            return;
        }

        lastWasCR = false;
        lineBuffer.write(value);
    }

    /**
     * Decodes and emits the currently buffered line if it is non-empty.
     *
     * <p>
     * The method assumes the caller holds {@link #lock}.
     * </p>
     */
    private void emitLineLocked() {
        if (lineBuffer.size() == 0) {
            return;
        }

        String line = lineBuffer.toString(charset);
        lineBuffer.reset();
        lineConsumer.accept(line);
    }
}
