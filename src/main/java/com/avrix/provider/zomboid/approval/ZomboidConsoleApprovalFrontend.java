package com.avrix.provider.zomboid.approval;

import com.avrix.provider.zomboid.approval.ui.ApprovalFormatting;

import java.io.BufferedReader;
import java.io.Console;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Console approval frontend for headless and dedicated-server launches.
 */
public final class ZomboidConsoleApprovalFrontend implements ZomboidApprovalFrontend {

    private static final long INPUT_POLL_MILLIS = 20L;

    private final BufferedReader reader;
    private final PrintWriter out;
    private final int timeoutSeconds;
    private boolean skipLineFeed;

    /**
     * Creates a console frontend using the process console or standard input.
     */
    public ZomboidConsoleApprovalFrontend() {
        this(createReader(), createWriter(), ApprovalTimeouts.readSeconds());
    }

    /**
     * Creates a console frontend with explicit streams, primarily for tests.
     *
     * @param reader         input reader
     * @param out            prompt writer
     * @param timeoutSeconds input timeout in seconds
     */
    public ZomboidConsoleApprovalFrontend(BufferedReader reader, PrintWriter out, int timeoutSeconds) {
        this.reader = Objects.requireNonNull(reader, "Reader cannot be null");
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
        out.println("  Mod ID: " + ApprovalFormatting.blankToDash(entry.modId()));
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
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(timeoutSeconds);
        StringBuilder line = new StringBuilder();
        while (!Thread.currentThread().isInterrupted() && System.nanoTime() < deadline) {
            // A cancelled blocking read can steal server commands after approval has already timed out.
            if (!reader.ready()) {
                try {
                    Thread.sleep(INPUT_POLL_MILLIS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return null;
                }
                continue;
            }
            int character = reader.read();
            if (character < 0) {
                return line.isEmpty() ? null : line.toString();
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
            line.append((char) character);
        }
        return null;
    }

    private static boolean isYes(String value) {
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return "y".equals(normalized) || "yes".equals(normalized);
    }

    private static BufferedReader createReader() {
        Console console = System.console();
        // Console.reader() does not promise readiness polling; stdin keeps timeout ownership on this thread.
        return new BufferedReader(new InputStreamReader(System.in,
                console == null ? StandardCharsets.UTF_8 : console.charset()));
    }

    private static PrintWriter createWriter() {
        Console console = System.console();
        if (console != null) {
            return new PrintWriter(console.writer(), true);
        }
        return new PrintWriter(System.out, true);
    }
}
