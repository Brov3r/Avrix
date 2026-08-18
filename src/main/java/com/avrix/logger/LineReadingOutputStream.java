package com.avrix.logger;

import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

/**
 * Thread-safe {@link OutputStream} that buffers incoming byte streams and emits complete lines
 * to a {@link Consumer} upon detecting standard line delimiters ({@code \r}, {@code \n}, or {@code \r\n}).
 * <p>
 * Decouples line emission from the internal lock to prevent deadlocks when consumer callbacks
 * interact with external logging subsystems.
 */
public final class LineReadingOutputStream extends OutputStream {

    private static final byte CR = '\r';
    private static final byte LF = '\n';
    private static final int INITIAL_CAPACITY = 256;

    private final ReentrantLock lock = new ReentrantLock();
    private final Consumer<String> lineConsumer;
    private final Charset charset;

    private byte[] buf = new byte[INITIAL_CAPACITY];
    private int count = 0;
    private boolean lastWasCR = false;

    /**
     * Constructs a new {@link LineReadingOutputStream} with a specified line consumer and character encoding.
     *
     * @param lineConsumer the consumer that receives emitted text lines, cannot be null
     * @param charset      the character encoding used to decode raw bytes, cannot be null
     * @throws NullPointerException if {@code lineConsumer} or {@code charset} is null
     */
    public LineReadingOutputStream(Consumer<String> lineConsumer, Charset charset) {
        this.lineConsumer = Objects.requireNonNull(lineConsumer, "lineConsumer cannot be null");
        this.charset = Objects.requireNonNull(charset, "charset cannot be null");
    }

    /**
     * Constructs a new {@link LineReadingOutputStream} using standard UTF-8 charset.
     *
     * @param lineConsumer the consumer that receives emitted text lines, cannot be null
     * @throws NullPointerException if {@code lineConsumer} is null
     */
    public LineReadingOutputStream(Consumer<String> lineConsumer) {
        this(lineConsumer, StandardCharsets.UTF_8);
    }

    /**
     * Writes a single byte to the stream and emits a line if a line delimiter is encountered.
     *
     * @param b the byte value to write (lower 8 bits)
     */
    @Override
    public void write(int b) {
        String lineToEmit;

        lock.lock();
        try {
            lineToEmit = processSingleByteLocked((byte) b);
        } finally {
            lock.unlock();
        }

        if (lineToEmit != null) {
            lineConsumer.accept(lineToEmit);
        }
    }

    /**
     * Writes a sub-array of bytes to the stream and emits all delimited lines encountered.
     *
     * @param bytes  the source byte array
     * @param offset the starting offset in the array
     * @param length the number of bytes to write
     * @throws NullPointerException      if {@code bytes} is null
     * @throws IndexOutOfBoundsException if {@code offset} or {@code length} is negative,
     *                                   or if {@code offset + length} exceeds array bounds
     */
    @Override
    public void write(byte[] bytes, int offset, int length) {
        Objects.requireNonNull(bytes, "bytes cannot be null");
        Objects.checkFromIndexSize(offset, length, bytes.length);

        if (length == 0) {
            return;
        }

        List<String> linesToEmit = new ArrayList<>();

        lock.lock();
        try {
            int end = offset + length;
            int sliceStart = offset;

            for (int i = offset; i < end; i++) {
                byte b = bytes[i];

                if (b == CR) {
                    appendBufLocked(bytes, sliceStart, i - sliceStart);
                    extractLineLocked(linesToEmit, true);
                    lastWasCR = true;
                    sliceStart = i + 1;
                } else if (b == LF) {
                    if (!lastWasCR) {
                        appendBufLocked(bytes, sliceStart, i - sliceStart);
                        extractLineLocked(linesToEmit, true);
                    }
                    lastWasCR = false;
                    sliceStart = i + 1;
                } else {
                    lastWasCR = false;
                }
            }

            if (sliceStart < end) {
                appendBufLocked(bytes, sliceStart, end - sliceStart);
            }
        } finally {
            lock.unlock();
        }

        for (String line : linesToEmit) {
            lineConsumer.accept(line);
        }
    }

    /**
     * Flushes any uncommitted characters remaining in the buffer as a complete line.
     */
    @Override
    public void flush() {
        String lineToEmit;

        lock.lock();
        try {
            lineToEmit = flushBufferLocked(false);
            lastWasCR = false;
        } finally {
            lock.unlock();
        }

        if (lineToEmit != null) {
            lineConsumer.accept(lineToEmit);
        }
    }

    /**
     * Closes the stream and flushes any pending buffered content.
     */
    @Override
    public void close() {
        flush();
    }

    /**
     * Processes a single byte under lock and returns the decoded line if a delimiter was hit.
     */
    private String processSingleByteLocked(byte value) {
        if (value == CR) {
            lastWasCR = true;
            return flushBufferLocked(true);
        }

        if (value == LF) {
            boolean skip = lastWasCR;
            lastWasCR = false;
            if (!skip) {
                return flushBufferLocked(true);
            }
            return null;
        }

        lastWasCR = false;
        ensureCapacityLocked(count + 1);
        buf[count++] = value;
        return null;
    }

    /**
     * Appends a sub-slice of bytes directly to the internal buffer.
     */
    private void appendBufLocked(byte[] src, int off, int len) {
        if (len <= 0) {
            return;
        }
        ensureCapacityLocked(count + len);
        System.arraycopy(src, off, buf, count, len);
        count += len;
    }

    /**
     * Expands internal buffer array if additional capacity is required.
     */
    private void ensureCapacityLocked(int minCapacity) {
        if (minCapacity - buf.length > 0) {
            int newCapacity = Math.max(buf.length << 1, minCapacity);
            buf = Arrays.copyOf(buf, newCapacity);
        }
    }

    /**
     * Extracts a line from the buffer and appends it to the emission collector.
     */
    private void extractLineLocked(List<String> accumulator, boolean isExplicitNewline) {
        String line = flushBufferLocked(isExplicitNewline);
        if (line != null) {
            accumulator.add(line);
        }
    }

    /**
     * Decodes the buffered bytes into a string and resets the buffer index.
     */
    private String flushBufferLocked(boolean isExplicitNewline) {
        if (!isExplicitNewline && count == 0) {
            return null;
        }
        String line = new String(buf, 0, count, charset);
        count = 0;
        return line;
    }
}