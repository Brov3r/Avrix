package com.avrix.provider.zomboid.approval;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Objects;

/**
 * Line-oriented request and response protocol used between the game process and the Swing approval process.
 * <p>
 * Free-form strings are Base64 encoded so paths and names cannot break the tab-separated framing. The protocol is
 * intentionally small and dependency-free.
 */
public final class ZomboidApprovalBatch {

    private static final String REQUEST_HEADER = "REQ\t1";
    private static final String RESPONSE_HEADER = "RESP\t1";
    private static final String ENTRY = "ENTRY";
    private static final String PERSIST = "PERSIST";
    private static final String DECISION = "DECISION";
    private static final String END = "END";

    private ZomboidApprovalBatch() {
        throw new UnsupportedOperationException("Protocol utility class cannot be instantiated");
    }

    /**
     * Writes one approval request.
     *
     * @param writer  destination writer
     * @param entries pending archives
     * @throws IOException if the request cannot be written or exceeds protocol limits
     */
    public static void writeRequest(Writer writer, List<ZomboidApprovalEntry> entries) throws IOException {
        Objects.requireNonNull(writer, "Writer cannot be null");
        List<ZomboidApprovalEntry> safeEntries = entries == null ? List.of() : entries;
        requireCount(safeEntries.size());
        writer = new LimitedWriter(writer, ApprovalLimits.MAX_REQUEST_CHARACTERS);

        writer.write(REQUEST_HEADER);
        writer.write('\n');
        for (ZomboidApprovalEntry entry : safeEntries) {
            writer.write(ENTRY);
            writeField(writer, entry.modId());
            writeField(writer, entry.pluginId());
            writeField(writer, entry.modAuthor());
            writeField(writer, entry.displayName());
            writeField(writer, entry.sourceType());
            writeField(writer, entry.sourceName());
            writeField(writer, entry.workshopId());
            writeField(writer, entry.jarPath().toString());
            writeField(writer, entry.sha256());
            writeField(writer, Long.toString(entry.size()));
            writeField(writer, Long.toString(entry.modifiedEpochMillis()));
            writeField(writer, entry.previousSha256());
            writeField(writer, String.join("\n", entry.mixinClasses()));
            writer.write('\n');
        }
        writer.write(END);
        writer.write('\n');
        writer.flush();
    }

    /**
     * Reads one approval request.
     *
     * @param reader source reader
     * @return pending archive entries
     * @throws IOException if the protocol is malformed, truncated, or exceeds its limits
     */
    public static List<ZomboidApprovalEntry> readRequest(Reader reader) throws IOException {
        Objects.requireNonNull(reader, "Reader cannot be null");
        ProtocolReader buffered = new ProtocolReader(reader, ApprovalLimits.MAX_REQUEST_CHARACTERS);
        String header = buffered.readLine();
        if (!REQUEST_HEADER.equals(header)) {
            throw new IOException("Bad approval request header: " + header);
        }

        List<ZomboidApprovalEntry> entries = new ArrayList<>();
        while (true) {
            String line = buffered.readLine();
            if (line == null) {
                throw new IOException("Approval request ended before END");
            }
            if (END.equals(line)) {
                return entries;
            }
            if (!line.startsWith(ENTRY + "\t")) {
                throw new IOException("Unexpected approval request line");
            }

            String[] parts = line.split("\t", -1);
            if (parts.length != 13 && parts.length != 14) {
                throw new IOException("Malformed approval request entry");
            }
            requireCount(entries.size() + 1);
            try {
                entries.add(new ZomboidApprovalEntry(
                    decode(parts[1]),
                    decode(parts[2]),
                    decode(parts[3]),
                    decode(parts[4]),
                    decode(parts[5]),
                    decode(parts[6]),
                    decode(parts[7]),
                    Path.of(decode(parts[8])),
                    decode(parts[9]),
                    parseLong(decode(parts[10])),
                    parseLong(decode(parts[11])),
                    decode(parts[12]),
                    parts.length == 14 ? decodeList(parts[13]) : List.of()
                ));
            } catch (IllegalArgumentException e) {
                throw new IOException("Invalid approval request entry: " + e.getMessage(), e);
            }
        }
    }

    /**
     * Writes one approval response.
     *
     * @param writer   destination writer
     * @param response response to write
     * @throws IOException if the response cannot be written or exceeds protocol limits
     */
    public static void writeResponse(Writer writer, ZomboidApprovalResponse response) throws IOException {
        Objects.requireNonNull(writer, "Writer cannot be null");
        ZomboidApprovalResponse safeResponse = response == null
                ? new ZomboidApprovalResponse(false, List.of())
                : response;
        requireCount(safeResponse.decisions().size());
        writer = new LimitedWriter(writer, ApprovalLimits.MAX_RESPONSE_CHARACTERS);

        writer.write(RESPONSE_HEADER);
        writer.write('\n');
        writer.write(PERSIST);
        writer.write('\t');
        writer.write(safeResponse.persist() ? "1" : "0");
        writer.write('\n');
        for (ZomboidApprovalDecision decision : safeResponse.decisions()) {
            writer.write(DECISION);
            writer.write('\t');
            writer.write(decision.sha256());
            writer.write('\t');
            writer.write(decision.allow() ? "1" : "0");
            writer.write('\n');
        }
        writer.write(END);
        writer.write('\n');
        writer.flush();
    }

    /**
     * Reads one approval response.
     *
     * @param reader source reader
     * @return parsed response
     * @throws IOException if the protocol is malformed, truncated, or exceeds its limits
     */
    public static ZomboidApprovalResponse readResponse(Reader reader) throws IOException {
        Objects.requireNonNull(reader, "Reader cannot be null");
        ProtocolReader buffered = new ProtocolReader(reader, ApprovalLimits.MAX_RESPONSE_CHARACTERS);
        String header = buffered.readLine();
        if (!RESPONSE_HEADER.equals(header)) {
            throw new IOException("Bad approval response header: " + header);
        }

        boolean persist = false;
        boolean persistSeen = false;
        List<ZomboidApprovalDecision> decisions = new ArrayList<>();
        while (true) {
            String line = buffered.readLine();
            if (line == null) {
                throw new IOException("Approval response ended before END");
            }
            if (END.equals(line)) {
                if (!persistSeen) {
                    throw new IOException("Approval response is missing PERSIST");
                }
                return new ZomboidApprovalResponse(persist, decisions);
            }
            if (line.startsWith(PERSIST + "\t")) {
                if (persistSeen) {
                    throw new IOException("Duplicate PERSIST line in approval response");
                }
                persist = parseBoolean(line.substring(PERSIST.length() + 1));
                persistSeen = true;
                continue;
            }
            if (line.startsWith(DECISION + "\t")) {
                String[] parts = line.split("\t", -1);
                if (parts.length != 3 || parts[1].isBlank()) {
                    throw new IOException("Malformed DECISION line in approval response");
                }
                requireCount(decisions.size() + 1);
                if (parts[1].length() > ApprovalLimits.MAX_IDENTIFIER_CHARACTERS) {
                    throw new IOException("Approval decision hash exceeds identity limit");
                }
                decisions.add(new ZomboidApprovalDecision(parts[1], parseBoolean(parts[2])));
                continue;
            }
            throw new IOException("Unexpected approval response line");
        }
    }

    private static void writeField(Writer writer, String value) throws IOException {
        writer.write('\t');
        writer.write(encode(value));
    }

    private static void requireCount(int count) throws IOException {
        if (count > ApprovalLimits.MAX_ENTRIES) {
            throw new IOException("Approval batch exceeds " + ApprovalLimits.MAX_ENTRIES + " entries");
        }
    }

    private static boolean parseBoolean(String value) throws IOException {
        if ("1".equals(value) || "true".equalsIgnoreCase(value)) {
            return true;
        }
        if ("0".equals(value) || "false".equalsIgnoreCase(value)) {
            return false;
        }
        throw new IOException("Invalid boolean value in approval protocol");
    }

    private static List<String> decodeList(String value) throws IOException {
        String decoded = decode(value);
        if (decoded.isBlank()) {
            return List.of();
        }
        return decoded.lines()
                .map(String::strip)
                .filter(line -> !line.isEmpty())
                .toList();
    }

    private static long parseLong(String value) throws IOException {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new IOException("Invalid number in approval protocol", e);
        }
    }

    private static String encode(String value) throws IOException {
        if (value == null || value.isEmpty()) {
            return "";
        }
        if (value.length() > ApprovalLimits.MAX_FIELD_CHARACTERS) {
            throw new IOException("Approval field exceeds decoded length limit");
        }
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String decode(String value) throws IOException {
        if (value == null || value.isEmpty()) {
            return "";
        }
        try {
            if (value.length() > ((ApprovalLimits.MAX_FIELD_CHARACTERS * 4 + 2) / 3) * 4) {
                throw new IOException("Encoded approval field exceeds length limit");
            }
            String decoded = new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
            if (decoded.length() > ApprovalLimits.MAX_FIELD_CHARACTERS) {
                throw new IOException("Approval field exceeds decoded length limit");
            }
            return decoded;
        } catch (IllegalArgumentException e) {
            throw new IOException("Invalid Base64 value in approval protocol", e);
        }
    }

    private static final class ProtocolReader {

        private final BufferedReader reader;
        private int remaining;
        private boolean skipLineFeed;

        private ProtocolReader(Reader reader, int limit) {
            this.reader = reader instanceof BufferedReader buffered ? buffered : new BufferedReader(reader);
            this.remaining = limit;
        }

        private String readLine() throws IOException {
            StringBuilder line = new StringBuilder();
            while (true) {
                int character = reader.read();
                if (character < 0) {
                    return line.isEmpty() ? null : line.toString();
                }
                if (--remaining < 0) {
                    throw new IOException("Approval message exceeds total length limit");
                }
                if (skipLineFeed) {
                    skipLineFeed = false;
                    if (character == '\n') {
                        continue;
                    }
                }
                if (character == '\r' || character == '\n') {
                    skipLineFeed = character == '\r';
                    return line.toString();
                }
                if (line.length() >= ApprovalLimits.MAX_LINE_CHARACTERS) {
                    throw new IOException("Approval line exceeds length limit");
                }
                line.append((char) character);
            }
        }
    }

    private static final class LimitedWriter extends java.io.FilterWriter {

        private int remaining;
        private int lineLength;

        private LimitedWriter(Writer writer, int limit) {
            super(writer);
            this.remaining = limit;
        }

        /**
         * Writes a single character while enforcing the configured output limits.
         *
         * @param character the character to write
         * @throws IOException if a limit is exceeded or the underlying writer fails
         */
        @Override
        public void write(int character) throws IOException {
            reserve(character);
            out.write(character);
        }

        /**
         * Writes a portion of a character array while enforcing the configured output limits.
         *
         * @param characters the character array to write from
         * @param offset the starting index in {@code characters}
         * @param length the number of characters to write
         * @throws IOException if a limit is exceeded or the underlying writer fails
         */
        @Override
        public void write(char[] characters, int offset, int length) throws IOException {
            for (int index = offset; index < offset + length; index++) {
                reserve(characters[index]);
            }
            out.write(characters, offset, length);
        }

        /**
         * Writes a portion of a string while enforcing the configured output limits.
         *
         * @param text the source string
         * @param offset the starting index in {@code text}
         * @param length the number of characters to write
         * @throws IOException if a limit is exceeded or the underlying writer fails
         */
        @Override
        public void write(String text, int offset, int length) throws IOException {
            for (int index = offset; index < offset + length; index++) {
                reserve(text.charAt(index));
            }
            out.write(text, offset, length);
        }

        private void reserve(int character) throws IOException {
            if (--remaining < 0) {
                throw new IOException("Approval message exceeds total length limit");
            }
            lineLength = character == '\n' || character == '\r' ? 0 : lineLength + 1;
            if (lineLength > ApprovalLimits.MAX_LINE_CHARACTERS) {
                throw new IOException("Approval line exceeds length limit");
            }
        }
    }
}
