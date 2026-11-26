package com.avrix.provider.zomboid.approval;

import com.avrix.provider.zomboid.approval.ui.ApprovalFormatting;

import java.io.BufferedReader;
import java.io.Console;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Console approval frontend for headless and dedicated-server launches.
 * <p>
 * Input is polled on the calling thread and standard input is read without read-ahead, so commands that follow the
 * current answer remain available to the game after approval.
 */
public final class ZomboidConsoleApprovalFrontend implements ZomboidApprovalFrontend {

    private static final long INPUT_POLL_MILLIS = 20L;
    private static final int NO_CHARACTER = -2;

    private final ConsoleInput input;
    private final PrintWriter out;
    private final int timeoutSeconds;
    private boolean skipLineFeed;

    /**
     * Creates a console frontend using the process console or standard input.
     */
    public ZomboidConsoleApprovalFrontend() {
        this(createInput(), createWriter(), ApprovalTimeouts.readSeconds());
    }

    /**
     * Creates a console frontend with explicit streams, primarily for tests.
     *
     * @param reader         input reader
     * @param out            prompt writer
     * @param timeoutSeconds input timeout in seconds
     */
    public ZomboidConsoleApprovalFrontend(BufferedReader reader, PrintWriter out, int timeoutSeconds) {
        this(new ReaderInput(Objects.requireNonNull(reader, "Reader cannot be null")), out, timeoutSeconds);
    }

    /**
     * Creates a console frontend reading raw bytes from the provided stream, primarily for tests.
     *
     * @param input          input stream
     * @param charset        stream charset
     * @param out            prompt writer
     * @param timeoutSeconds input timeout in seconds
     */
    ZomboidConsoleApprovalFrontend(InputStream input, Charset charset, PrintWriter out, int timeoutSeconds) {
        this(new StreamInput(
                Objects.requireNonNull(input, "Input stream cannot be null"),
                Objects.requireNonNull(charset, "Charset cannot be null")
        ), out, timeoutSeconds);
    }

    private ZomboidConsoleApprovalFrontend(ConsoleInput input, PrintWriter out, int timeoutSeconds) {
        this.input = input;
        this.out = Objects.requireNonNull(out, "Writer cannot be null");
        this.timeoutSeconds = Math.max(1, Math.min(ApprovalTimeouts.MAX_SECONDS, timeoutSeconds));
    }

    /**
     * Returns whether this frontend can ask the user interactively.
     *
     * @return always {@code true}
     */
    @Override
    public boolean supportsInteractivePrompt() {
        return true;
    }

    /**
     * Asks the user to approve or deny each pending archive.
     *
     * @param pending archives awaiting approval
     * @return user decisions, or an empty response when the input is closed or times out
     */
    @Override
    public ZomboidApprovalResponse request(List<ZomboidApprovalEntry> pending) {
        if (pending == null || pending.isEmpty()) {
            return new ZomboidApprovalResponse(false, List.of());
        }

        List<ZomboidApprovalDecision> decisions = new ArrayList<>(pending.size());
        boolean allowAll = false;
        boolean denyAll = false;

        try {
            out.println();
            out.println("=== Avrix plugin approval ===");
            for (int index = 0; index < pending.size(); index++) {
                ZomboidApprovalEntry entry = pending.get(index);
                printEntry(entry, index + 1, pending.size());

                boolean allow;
                if (allowAll) {
                    allow = true;
                } else if (denyAll) {
                    allow = false;
                } else {
                    out.print("Allow this plugin? [y]es / [N]o / [a]llow all / [d]eny all / [q]uit: ");
                    out.flush();
                    String answer = readLine();
                    if (answer == null) {
                        out.println();
                        out.println("No approval input; all pending plugins will be denied.");
                        return new ZomboidApprovalResponse(false, List.of());
                    }

                    String normalized = answer.trim().toLowerCase(Locale.ROOT);
                    if ("q".equals(normalized) || "quit".equals(normalized) || "cancel".equals(normalized)) {
                        out.println("Approval cancelled; all pending plugins will be denied.");
                        return new ZomboidApprovalResponse(false, List.of());
                    }
                    if ("a".equals(normalized) || "all".equals(normalized)
                            || "allow-all".equals(normalized) || "allowall".equals(normalized)) {
                        allowAll = true;
                        allow = true;
                    } else if ("d".equals(normalized) || "deny-all".equals(normalized)
                            || "denyall".equals(normalized)) {
                        denyAll = true;
                        allow = false;
                    } else {
                        allow = "y".equals(normalized) || "yes".equals(normalized) || "allow".equals(normalized);
                    }
                }

                decisions.add(new ZomboidApprovalDecision(entry.sha256(), allow));
                out.println(allow ? "  -> allowed" : "  -> denied");
            }

            out.print("Remember these decisions for future launches? [y]es / [N]o: ");
            out.flush();
            String remember = readLine();
            boolean persist = remember != null && isYes(remember);
            return new ZomboidApprovalResponse(persist, decisions);
        } catch (IOException e) {
            out.println("Failed to read approval input; all pending plugins will be denied.");
            return new ZomboidApprovalResponse(false, List.of());
        }
    }

    private void printEntry(ZomboidApprovalEntry entry, int index, int total) {
        String displayName = entry.displayName().isBlank() ? entry.pluginId() : entry.displayName();
        out.printf("[%d/%d] %s%n", index, total, displayName);
        out.println("  Plugin ID: " + ApprovalFormatting.blankToDash(entry.pluginId()));
        out.println("  Mod ID: " + ApprovalFormatting.blankToDash(entry.displayModId()));
        out.println("  Author: " + ApprovalFormatting.blankToDash(entry.modAuthor()));
        out.println("  Source: " + describeSource(entry));
        out.println("  Size: " + ApprovalFormatting.formatSize(entry.size()));
        out.println("  SHA-256: " + entry.sha256());
        if (!entry.previousSha256().isBlank()) {
            out.println("  Previously approved: " + entry.previousSha256());
        }
    }

    private static String describeSource(ZomboidApprovalEntry entry) {
        if (!entry.workshopId().isBlank()) {
            return "Workshop " + entry.workshopId();
        }
        if (!entry.sourceName().isBlank()) {
            return entry.sourceName();
        }
        return entry.sourceType().isBlank() ? "local" : entry.sourceType();
    }

    private String readLine() throws IOException {
        long deadlineNanos = System.nanoTime() + TimeUnit.SECONDS.toNanos(timeoutSeconds);
        StringBuilder line = new StringBuilder();
        while (!Thread.currentThread().isInterrupted() && System.nanoTime() < deadlineNanos) {
            // Poll instead of blocking: a cancelled blocking read can steal server commands after the deadline.
            if (!input.ready()) {
                if (!pauseBeforeNextPoll()) {
                    return null;
                }
                continue;
            }
            int character = input.read();
            if (character == NO_CHARACTER) {
                continue;
            }
            if (character < 0) {
                return line.isEmpty() ? null : line.toString();
            }
            if (skipLineFeed) {
                skipLineFeed = false;
                if (character == '\n') {
                    continue;
                }
            }
            if (character == '\r') {
                // Consume the LF of CRLF only when it is already available; a CR-only line must keep its next byte.
                if (!input.consumeLineFeedIfAvailable()) {
                    skipLineFeed = true;
                }
                return line.toString();
            }
            if (character == '\n') {
                return line.toString();
            }
            line.append((char) character);
        }
        return null;
    }

    private static boolean pauseBeforeNextPoll() {
        try {
            Thread.sleep(INPUT_POLL_MILLIS);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * Supplies one decoded character at a time without reading past the current line.
     */
    private interface ConsoleInput {

        /**
         * Returns whether a character or encoded bytes are available without blocking.
         *
         * @return {@code true} when {@link #read()} can make progress
         * @throws IOException if readiness cannot be checked
         */
        boolean ready() throws IOException;

        /**
         * Reads one decoded character.
         *
         * @return the character, {@code -1} at end of input, or a sentinel when more bytes are needed
         * @throws IOException if the input cannot be read
         */
        int read() throws IOException;

        /**
         * Consumes an LF that immediately follows CR when it is already available.
         *
         * @return {@code true} when the LF was consumed, {@code false} when the next input must stay untouched
         * @throws IOException if the input cannot be read
         */
        boolean consumeLineFeedIfAvailable() throws IOException;
    }

    /**
     * Reads characters from a caller-provided reader.
     */
    private static final class ReaderInput implements ConsoleInput {

        private final BufferedReader reader;

        private ReaderInput(BufferedReader reader) {
            this.reader = reader;
        }

        /**
         * Returns whether the wrapped reader has a character available without blocking.
         *
         * @return {@code true} when a character can be read
         * @throws IOException if readiness cannot be checked
         */
        @Override
        public boolean ready() throws IOException {
            return reader.ready();
        }

        /**
         * Reads one character from the wrapped reader.
         *
         * @return the character, or {@code -1} at end of input
         * @throws IOException if the reader cannot be read
         */
        @Override
        public int read() throws IOException {
            return reader.read();
        }

        /**
         * Consumes an LF that immediately follows CR when the reader already has it.
         *
         * @return {@code true} when the LF was consumed, {@code false} when the next character must stay untouched
         * @throws IOException if the reader cannot be read
         */
        @Override
        public boolean consumeLineFeedIfAvailable() throws IOException {
            if (!reader.markSupported() || !reader.ready()) {
                return false;
            }
            reader.mark(1);
            int next = reader.read();
            if (next == '\n') {
                return true;
            }
            if (next >= 0) {
                reader.reset();
            }
            return false;
        }
    }

    /**
     * Reads stdin one byte at a time and decodes it without buffering input beyond the current character.
     */
    static final class StreamInput implements ConsoleInput {

        private final InputStream input;
        private final byte[] lineFeed;
        private final CharsetDecoder decoder;
        private final ByteBuffer encoded = ByteBuffer.allocate(64);
        // One input byte yields at most a small number of chars (a surrogate pair yields two).
        private final CharBuffer decoded = CharBuffer.allocate(8);
        private boolean endOfInput;

        StreamInput(InputStream input, Charset charset) {
            this.input = input;
            this.lineFeed = lineFeedBytes(charset);
            this.decoder = charset.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPLACE)
                    .onUnmappableCharacter(CodingErrorAction.REPLACE);
            // A fresh buffer is writable, but no decoded characters exist yet.
            decoded.limit(0);
        }

        /**
         * Returns whether a decoded character, encoded bytes, or end of input is available.
         *
         * @return {@code true} when a character or end marker can be read without blocking
         * @throws IOException if readiness cannot be checked
         */
        @Override
        public boolean ready() throws IOException {
            return endOfInput || decoded.hasRemaining() || input.available() > 0;
        }

        /**
         * Reads one decoded character, decoding at most the bytes currently available.
         *
         * @return the character, {@code -1} at end of input, or a sentinel when more bytes are needed
         * @throws IOException if the stream cannot be read
         */
        @Override
        public int read() throws IOException {
            if (decoded.hasRemaining()) {
                return decoded.get();
            }
            while (!endOfInput && input.available() > 0) {
                int value = input.read();
                if (value < 0) {
                    endOfInput = true;
                    decode(true);
                    break;
                }
                encoded.put((byte) value);
                decode(false);
                if (decoded.hasRemaining()) {
                    return decoded.get();
                }
            }
            if (decoded.hasRemaining()) {
                return decoded.get();
            }
            return endOfInput ? -1 : NO_CHARACTER;
        }

        /**
         * Consumes an LF that immediately follows CR when its exact encoded bytes are already available.
         *
         * @return {@code true} when the LF was consumed, {@code false} when the next input must stay untouched
         * @throws IOException if the stream cannot be read
         */
        @Override
        public boolean consumeLineFeedIfAvailable() throws IOException {
            if (decoded.hasRemaining()
                    || lineFeed.length == 0
                    || !input.markSupported()
                    || input.available() < lineFeed.length) {
                return false;
            }
            // mark/reset keeps a CR-only command byte in the shared stream; available() prevents blocking.
            input.mark(lineFeed.length);
            byte[] candidate = new byte[lineFeed.length];
            int read = input.read(candidate, 0, candidate.length);
            if (read == candidate.length && Arrays.equals(candidate, lineFeed)) {
                return true;
            }
            if (read >= 0) {
                input.reset();
            }
            return false;
        }

        private void decode(boolean end) {
            encoded.flip();
            decoded.clear();
            decoder.decode(encoded, decoded, end);
            if (end) {
                decoder.flush(decoded);
            }
            encoded.compact();
            decoded.flip();
        }
    }

    private static boolean isYes(String value) {
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return "y".equals(normalized) || "yes".equals(normalized);
    }

    private static ConsoleInput createInput() {
        Console console = System.console();
        boolean terminal = console != null && console.isTerminal();
        // System.in is shared with the game; byte reads keep later commands out of private read-ahead buffers.
        return new StreamInput(System.in, stdinCharset(terminal));
    }

    static Charset stdinCharset(boolean terminal) {
        if (!terminal) {
            // Keep the historical UTF-8 decoding for piped and redirected input.
            return StandardCharsets.UTF_8;
        }
        // Console.charset() describes console output; stdin.encoding is the documented input charset.
        String name = System.getProperty("stdin.encoding");
        if (name == null || name.isBlank()) {
            return StandardCharsets.UTF_8;
        }
        try {
            return Charset.forName(name, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return StandardCharsets.UTF_8;
        }
    }

    private static byte[] lineFeedBytes(Charset charset) {
        if (!canConsumeEncodedLineFeed(charset)) {
            // Without proof of the exact LF bytes, leave CRLF handling to the next read/skip.
            return new byte[0];
        }
        ByteBuffer encodedLineFeed = charset.encode(CharBuffer.wrap("\n"));
        byte[] bytes = new byte[encodedLineFeed.remaining()];
        encodedLineFeed.get(bytes);
        return bytes;
    }

    private static boolean canConsumeEncodedLineFeed(Charset charset) {
        if (charset.equals(StandardCharsets.UTF_8)
                || charset.equals(StandardCharsets.UTF_16LE)
                || charset.equals(StandardCharsets.UTF_16BE)
                || charset.equals(StandardCharsets.UTF_32LE)
                || charset.equals(StandardCharsets.UTF_32BE)) {
            return true;
        }
        try {
            // Single-byte encodings encode LF as one unambiguous byte.
            return charset.newEncoder().maxBytesPerChar() <= 1.0f;
        } catch (UnsupportedOperationException e) {
            return false;
        }
    }

    private static PrintWriter createWriter() {
        Console console = System.console();
        if (console != null && console.isTerminal()) {
            return new PrintWriter(console.writer(), true);
        }
        return new PrintWriter(System.out, true);
    }
}
