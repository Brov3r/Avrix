package com.avrix.provider.zomboid.approval;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PipedReader;
import java.io.PipedWriter;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that console timeouts do not leave outstanding input reads.
 */
public class ConsoleApprovalTimeoutTest {

    /**
     * Verifies that commands submitted after a timeout remain available to the server.
     *
     * @throws IOException if the input pipe cannot be used
     */
    @Test
    public void shouldLeaveSubsequentServerCommandUnread() throws IOException {
        try (var pipe = new PipedReader();
             var writer = new PipedWriter(pipe);
             var reader = new BufferedReader(pipe)) {
            var frontend = frontend(reader);
            writer.write("unfinished");
            writer.flush();

            assertThat(frontend.request(List.of(entry())).decisions()).isEmpty();

            writer.write("status\n");
            writer.flush();
            assertThat(reader.readLine()).isEqualTo("status");
        }
    }

    /**
     * Verifies that CRLF input does not introduce an empty answer for the next prompt.
     */
    @Test
    public void shouldReadCrLfAnswers() {
        var frontend = frontend(new BufferedReader(new StringReader("y\r\ny\r\n")));
        var response = frontend.request(List.of(entry()));

        assertThat(response.persist()).isTrue();
        assertThat(response.decisions()).containsExactly(new ZomboidApprovalDecision("a".repeat(64), true));
    }

    /**
     * Verifies that interruption returns without consuming input and preserves the interrupt flag.
     *
     * @throws IOException if the scripted input cannot be read
     */
    @Test
    public void shouldNotReadWhenInterrupted() throws IOException {
        var reader = new BufferedReader(new StringReader("status\n"));
        Thread.currentThread().interrupt();
        try {
            assertThat(frontend(reader).request(List.of(entry())).decisions()).isEmpty();
            assertThat(Thread.currentThread().isInterrupted()).isTrue();
        } finally {
            Thread.interrupted();
        }
        assertThat(reader.readLine()).isEqualTo("status");
    }

    /**
     * Verifies that stream input keeps complete lines after the current answer for the server.
     */
    @Test
    public void shouldLeaveFollowingStreamLinesUnread() {
        ByteArrayInputStream input = new ByteArrayInputStream(
                "n\ny\nstatus\n".getBytes(StandardCharsets.UTF_8)
        );

        ZomboidApprovalResponse response = streamFrontend(input).request(List.of(entry()));

        assertThat(response.persist()).isTrue();
        assertThat(response.decisions())
                .containsExactly(new ZomboidApprovalDecision("a".repeat(64), false));
        assertThat(new String(input.readAllBytes(), StandardCharsets.UTF_8)).isEqualTo("status\n");
    }

    /**
     * Verifies that CRLF input does not introduce an empty answer for the next prompt on stream input.
     */
    @Test
    public void shouldReadCrLfAnswersFromStream() {
        ByteArrayInputStream input = new ByteArrayInputStream(
                "y\r\ny\r\n".getBytes(StandardCharsets.UTF_8)
        );

        ZomboidApprovalResponse response = streamFrontend(input).request(List.of(entry()));

        assertThat(response.persist()).isTrue();
        assertThat(response.decisions())
                .containsExactly(new ZomboidApprovalDecision("a".repeat(64), true));
        assertThat(input.readAllBytes()).isEmpty();
    }

    /**
     * Verifies that the LF of CRLF is consumed after the final answer instead of leaking into the next command.
     */
    @Test
    public void shouldConsumeCrLfAfterFinalStreamAnswer() {
        ByteArrayInputStream input = new ByteArrayInputStream(
                "n\ny\r\nstatus\r\n".getBytes(StandardCharsets.UTF_8)
        );

        ZomboidApprovalResponse response = streamFrontend(input).request(List.of(entry()));

        assertThat(response.persist()).isTrue();
        assertThat(new String(input.readAllBytes(), StandardCharsets.UTF_8)).isEqualTo("status\r\n");
    }

    /**
     * Verifies that a CR-only answer does not consume the first byte of the next command.
     */
    @Test
    public void shouldKeepFirstByteAfterCrOnlyStreamAnswer() {
        ByteArrayInputStream input = new ByteArrayInputStream(
                "n\ny\rstatus\r\n".getBytes(StandardCharsets.UTF_8)
        );

        ZomboidApprovalResponse response = streamFrontend(input).request(List.of(entry()));

        assertThat(response.persist()).isTrue();
        assertThat(new String(input.readAllBytes(), StandardCharsets.UTF_8)).isEqualTo("status\r\n");
    }

    /**
     * Verifies that a CR-only answer on a pipe also keeps the next command intact.
     *
     * @throws IOException if the input pipe cannot be used
     */
    @Test
    public void shouldKeepFirstByteAfterCrOnlyPipeAnswer() throws IOException {
        try (PipedInputStream pipe = new PipedInputStream();
             PipedOutputStream writer = new PipedOutputStream(pipe)) {
            writer.write("n\ny\r".getBytes(StandardCharsets.UTF_8));
            writer.flush();

            ZomboidApprovalResponse response = streamFrontend(pipe).request(List.of(entry()));

            assertThat(response.persist()).isTrue();

            writer.write("status\r\n".getBytes(StandardCharsets.UTF_8));
            writer.flush();
            assertThat(new String(pipe.readNBytes(8), StandardCharsets.UTF_8)).isEqualTo("status\r\n");
        }
    }

    /**
     * Verifies that CRLF remains a single line terminator for explicit multi-byte charsets.
     */
    @Test
    public void shouldConsumeMultibyteCrLfSeparators() {
        for (Charset charset : List.of(StandardCharsets.UTF_16LE, StandardCharsets.UTF_16BE)) {
            ByteArrayInputStream input = new ByteArrayInputStream(
                    "n\r\ny\r\nstatus\r\n".getBytes(charset)
            );

            ZomboidApprovalResponse response = new ZomboidConsoleApprovalFrontend(
                    input, charset, new PrintWriter(new StringWriter()), 1
            ).request(List.of(entry()));

            assertThat(response.persist()).as(charset.name()).isTrue();
            assertThat(response.decisions()).as(charset.name())
                    .containsExactly(new ZomboidApprovalDecision("a".repeat(64), false));
            assertThat(new String(input.readAllBytes(), charset)).as(charset.name())
                    .isEqualTo("status\r\n");
        }
    }

    /**
     * Verifies that BOM-based UTF-16 decodes little-endian input without deleting unproven bytes.
     */
    @Test
    public void shouldDecodeBomBasedUtf16LittleEndianSafely() {
        byte[] payload = "y\r\ny\r\nstatus\r\n".getBytes(StandardCharsets.UTF_16LE);
        byte[] inputBytes = new byte[payload.length + 2];
        inputBytes[0] = (byte) 0xFF;
        inputBytes[1] = (byte) 0xFE;
        System.arraycopy(payload, 0, inputBytes, 2, payload.length);
        ByteArrayInputStream input = new ByteArrayInputStream(inputBytes);

        ZomboidApprovalResponse response = new ZomboidConsoleApprovalFrontend(
                input, StandardCharsets.UTF_16, new PrintWriter(new StringWriter()), 1
        ).request(List.of(entry()));

        assertThat(response.persist()).isTrue();
        assertThat(response.decisions())
                .containsExactly(new ZomboidApprovalDecision("a".repeat(64), true));
        String remaining = new String(input.readAllBytes(), StandardCharsets.UTF_16LE);
        assertThat(remaining).isEqualTo("\nstatus\r\n");
    }

    /**
     * Verifies that an empty stream does not fabricate characters before any input arrives.
     *
     * @throws IOException if readiness cannot be checked
     */
    @Test
    public void shouldNotReadyBeforeInputArrives() throws IOException {
        ZomboidConsoleApprovalFrontend.StreamInput input = new ZomboidConsoleApprovalFrontend.StreamInput(
                new ByteArrayInputStream(new byte[0]), StandardCharsets.UTF_8
        );

        assertThat(input.ready()).isFalse();
    }

    /**
     * Verifies that a missing or invalid {@code stdin.encoding} falls back to UTF-8.
     */
    @Test
    public void shouldFallBackToUtf8WithoutValidStdinEncoding() {
        String previous = System.getProperty("stdin.encoding");
        try {
            assertThat(ZomboidConsoleApprovalFrontend.stdinCharset(false))
                    .isEqualTo(StandardCharsets.UTF_8);

            System.clearProperty("stdin.encoding");
            assertThat(ZomboidConsoleApprovalFrontend.stdinCharset(true))
                    .isEqualTo(StandardCharsets.UTF_8);

            System.setProperty("stdin.encoding", "  ");
            assertThat(ZomboidConsoleApprovalFrontend.stdinCharset(true))
                    .isEqualTo(StandardCharsets.UTF_8);

            System.setProperty("stdin.encoding", "not a charset!");
            assertThat(ZomboidConsoleApprovalFrontend.stdinCharset(true))
                    .isEqualTo(StandardCharsets.UTF_8);
        } finally {
            if (previous == null) {
                System.clearProperty("stdin.encoding");
            } else {
                System.setProperty("stdin.encoding", previous);
            }
        }
    }

    /**
     * Verifies that stream input written after a timeout remains readable by the server.
     *
     * @throws IOException if the input pipe cannot be used
     */
    @Test
    public void shouldNotConsumeStreamInputAfterTimeout() throws IOException {
        try (PipedInputStream pipe = new PipedInputStream();
             PipedOutputStream writer = new PipedOutputStream(pipe)) {
            ZomboidApprovalResponse response = streamFrontend(pipe).request(List.of(entry()));

            assertThat(response.decisions()).isEmpty();

            writer.write("status\n".getBytes(StandardCharsets.UTF_8));
            writer.flush();
            assertThat(new String(pipe.readNBytes(7), StandardCharsets.UTF_8)).isEqualTo("status\n");
        }
    }

    private static ZomboidConsoleApprovalFrontend frontend(BufferedReader reader) {
        return new ZomboidConsoleApprovalFrontend(reader, new PrintWriter(new StringWriter()), 1);
    }

    private static ZomboidConsoleApprovalFrontend streamFrontend(InputStream input) {
        return new ZomboidConsoleApprovalFrontend(
                input, StandardCharsets.UTF_8, new PrintWriter(new StringWriter()), 1);
    }

    private static ZomboidApprovalEntry entry() {
        return new ZomboidApprovalEntry("mod", "plugin", "author", "Plugin", "mod", "Mod", "",
                Path.of("plugin.jar"), "a".repeat(64), 0L, 0L, "", List.of());
    }
}
