package com.avrix.provider.zomboid.approval;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PipedReader;
import java.io.PipedWriter;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
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

    private static ZomboidConsoleApprovalFrontend frontend(BufferedReader reader) {
        return new ZomboidConsoleApprovalFrontend(reader, new PrintWriter(new StringWriter()), 1);
    }

    private static ZomboidApprovalEntry entry() {
        return new ZomboidApprovalEntry("mod", "plugin", "author", "Plugin", "mod", "Mod", "",
                Path.of("plugin.jar"), "a".repeat(64), 0L, 0L, "", List.of());
    }
}
