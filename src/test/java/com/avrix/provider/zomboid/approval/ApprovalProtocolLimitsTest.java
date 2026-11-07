package com.avrix.provider.zomboid.approval;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies bounded metadata presentation, protocol messages, and pre-approval class inspection.
 */
public class ApprovalProtocolLimitsTest {

    @TempDir
    private Path directory;

    /** Verifies that only presentation text is truncated, preserving approval identity. */
    @Test
    public void shouldBoundDisplayTextWithoutChangingIdentity() {
        var entry = entry("Author".repeat(10000));
        assertThat(entry.modAuthor()).hasSize(ApprovalLimits.MAX_DISPLAY_CHARACTERS).endsWith("…");
        assertThat(entry.sha256()).isEqualTo("a".repeat(64));
        assertThatThrownBy(() -> new ZomboidApprovalEntry("m".repeat(ApprovalLimits.MAX_IDENTIFIER_CHARACTERS + 1),
                "plugin", "Author", "Plugin", "mod", "Mod", "", directory.resolve("plugin.jar"),
                "a".repeat(64), 0L, 0L, "", List.of())).isInstanceOf(IllegalArgumentException.class);
        assertThat(ApprovalLimits.displayText("x".repeat(1022) + "🌍extra")).doesNotContain("\uD83C");
    }

    /**
     * Verifies that both readers stop oversized lines before accumulating an unbounded string.
     */
    @Test
    public void shouldRejectOversizedProtocolLines() {
        String line = "x".repeat(ApprovalLimits.MAX_LINE_CHARACTERS + 1);
        assertThatThrownBy(() -> ZomboidApprovalBatch.readRequest(new StringReader(line)))
                .isInstanceOf(IOException.class).hasMessageContaining("line exceeds");
        assertThatThrownBy(() -> ZomboidApprovalBatch.readResponse(new StringReader(line)))
                .isInstanceOf(IOException.class).hasMessageContaining("line exceeds");
    }

    /**
     * Verifies batch counts and aggregate request limits on both writing and reading.
     *
     * @throws IOException if a fixture request cannot be encoded
     */
    @Test
    public void shouldRejectOversizedBatchesAndMessages() throws IOException {
        var entry = entry("Author");
        assertThatThrownBy(() -> ZomboidApprovalBatch.writeRequest(new StringWriter(),
                Collections.nCopies(ApprovalLimits.MAX_ENTRIES + 1, entry)))
                .isInstanceOf(IOException.class).hasMessageContaining("batch exceeds");
        StringWriter encoded = new StringWriter();
        ZomboidApprovalBatch.writeRequest(encoded, List.of(entry));
        String request = encoded.toString();
        String row = request.substring(request.indexOf("ENTRY"), request.lastIndexOf("END"));
        assertThatThrownBy(() -> ZomboidApprovalBatch.readRequest(new StringReader("REQ\t1\n"
                + row.repeat(ApprovalLimits.MAX_ENTRIES + 1) + "END\n")))
                .isInstanceOf(IOException.class).hasMessageContaining("batch exceeds");

        var verbose = entry("Author".repeat(1000));
        assertThatThrownBy(() -> ZomboidApprovalBatch.writeRequest(WriterDiscard.INSTANCE,
                Collections.nCopies(ApprovalLimits.MAX_ENTRIES, verbose)))
                .isInstanceOf(IOException.class).hasMessageContaining("total length");
        StringWriter verboseRequest = new StringWriter();
        ZomboidApprovalBatch.writeRequest(verboseRequest, List.of(verbose));
        String verboseRow = verboseRequest.toString().substring(verboseRequest.toString().indexOf("ENTRY"),
                verboseRequest.toString().lastIndexOf("END"));
        int repetitions = ApprovalLimits.MAX_REQUEST_CHARACTERS / verboseRow.length() + 1;
        assertThatThrownBy(() -> ZomboidApprovalBatch.readRequest(new StringReader("REQ\t1\n"
                + verboseRow.repeat(repetitions) + "END\n")))
                .isInstanceOf(IOException.class).hasMessageContaining("total length");
    }

    /**
     * Verifies decoded field bounds and decision-count bounds independently of line length.
     *
     * @throws IOException if a fixture request cannot be encoded
     */
    @Test
    public void shouldBoundDecodedFieldsAndResponseCounts() throws IOException {
        StringWriter encoded = new StringWriter();
        ZomboidApprovalBatch.writeRequest(encoded, List.of(entry("Author")));
        String[] parts = encoded.toString().lines().skip(1).findFirst().orElseThrow().split("\t", -1);
        parts[3] = java.util.Base64.getEncoder().encodeToString("x".repeat(ApprovalLimits.MAX_FIELD_CHARACTERS + 1)
                .getBytes(java.nio.charset.StandardCharsets.UTF_8));
        assertThatThrownBy(() -> ZomboidApprovalBatch.readRequest(new StringReader("REQ\t1\n"
                + String.join("\t", parts) + "\nEND\n")))
                .isInstanceOf(IOException.class).hasMessageContaining("decoded length");

        var decision = new ZomboidApprovalDecision("a".repeat(64), true);
        assertThatThrownBy(() -> ZomboidApprovalBatch.writeResponse(new StringWriter(), new ZomboidApprovalResponse(false,
                Collections.nCopies(ApprovalLimits.MAX_ENTRIES + 1, decision))))
                .isInstanceOf(IOException.class).hasMessageContaining("batch exceeds");
        assertThatThrownBy(() -> ZomboidApprovalBatch.readResponse(new StringReader("RESP\t1\nPERSIST\t0\n"
                + ("DECISION\t" + decision.sha256() + "\t1\n").repeat(ApprovalLimits.MAX_ENTRIES + 1) + "END\n")))
                .isInstanceOf(IOException.class).hasMessageContaining("batch exceeds");
    }

    /**
     * Verifies CRLF framing remains compatible with the bounded reader.
     *
     * @throws IOException if the protocol cannot be encoded or decoded
     */
    @Test
    public void shouldPreserveCrLfRoundTrip() throws IOException {
        var output = new StringWriter();
        var entry = entry("Author");
        ZomboidApprovalBatch.writeRequest(output, List.of(entry));
        assertThat(ZomboidApprovalBatch.readRequest(new StringReader(output.toString().replace("\n", "\r\n"))))
                .containsExactly(entry);
    }

    /**
     * Verifies that a highly compressed oversized class cannot force unbounded allocation during inspection.
     *
     * @throws IOException if a fixture archive cannot be created
     */
    @Test
    public void shouldBoundUncompressedInspectionBytes() throws IOException {
        Path archive = directory.resolve("large.jar");
        try (var output = new JarOutputStream(Files.newOutputStream(archive))) {
            output.putNextEntry(new JarEntry("example/Large.class"));
            output.write(new byte[ApprovalLimits.MAX_CLASS_BYTES + 1]);
            output.closeEntry();
        }
        assertThat(MixinTargetInspector.inspect(archive, List.of("example.Large")).issues())
                .anySatisfy(issue -> assertThat(issue).contains("inspection byte limit"));
    }

    /** Verifies that annotation-derived names and diagnostics cannot bypass presentation limits. */
    @Test
    public void shouldBoundInspectionPresentation() {
        String value = "x".repeat(ApprovalLimits.MAX_DISPLAY_CHARACTERS + 100);
        var result = new MixinTargetInspector.Result(List.of(),
                Collections.nCopies(ApprovalLimits.MAX_MIXINS + 1, value), List.of(value));
        assertThat(result.targetClasses()).hasSize(ApprovalLimits.MAX_MIXINS)
                .allSatisfy(text -> assertThat(text).hasSize(ApprovalLimits.MAX_DISPLAY_CHARACTERS));
        assertThat(result.issues()).singleElement().asString().endsWith("…");
    }

    private ZomboidApprovalEntry entry(String author) {
        return new ZomboidApprovalEntry("mod", "plugin", author, author, "mod", author, "",
                directory.resolve("plugin.jar"), "a".repeat(64), 0L, 0L, "", List.of());
    }

    private static final class WriterDiscard extends java.io.Writer {

        private static final WriterDiscard INSTANCE = new WriterDiscard();

        /** {@inheritDoc} */
        @Override
        public void write(char[] characters, int offset, int length) {
        }

        /** {@inheritDoc} */
        @Override
        public void flush() {
        }

        /** {@inheritDoc} */
        @Override
        public void close() {
        }
    }
}
