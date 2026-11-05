package com.avrix.provider.approval;

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
     * @throws IOException if the request cannot be written
     */
    public static void writeRequest(Writer writer, List<ZomboidApprovalEntry> entries) throws IOException {
        Objects.requireNonNull(writer, "Writer cannot be null");
        List<ZomboidApprovalEntry> safeEntries = entries == null ? List.of() : entries;

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
     * @throws IOException if the protocol is malformed or truncated
     */
    public static List<ZomboidApprovalEntry> readRequest(Reader reader) throws IOException {
        Objects.requireNonNull(reader, "Reader cannot be null");
        BufferedReader buffered = asBufferedReader(reader);
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
            if (parts.length != 13) {
                throw new IOException("Malformed approval request entry");
            }
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
                    decode(parts[12])
            ));
        }
    }

    /**
     * Writes one approval response.
     *
     * @param writer   destination writer
     * @param response response to write
     * @throws IOException if the response cannot be written
     */
    public static void writeResponse(Writer writer, ZomboidApprovalResponse response) throws IOException {
        Objects.requireNonNull(writer, "Writer cannot be null");
        ZomboidApprovalResponse safeResponse = response == null
                ? new ZomboidApprovalResponse(false, List.of())
                : response;

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
     * @throws IOException if the protocol is malformed or truncated
     */
    public static ZomboidApprovalResponse readResponse(Reader reader) throws IOException {
        Objects.requireNonNull(reader, "Reader cannot be null");
        BufferedReader buffered = asBufferedReader(reader);
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

    private static BufferedReader asBufferedReader(Reader reader) {
        return reader instanceof BufferedReader buffered ? buffered : new BufferedReader(reader);
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

    private static long parseLong(String value) throws IOException {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new IOException("Invalid number in approval protocol", e);
        }
    }

    private static String encode(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String decode(String value) throws IOException {
        if (value == null || value.isEmpty()) {
            return "";
        }
        try {
            return new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            throw new IOException("Invalid Base64 value in approval protocol", e);
        }
    }
}
