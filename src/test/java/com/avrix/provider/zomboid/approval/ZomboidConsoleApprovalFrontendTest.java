package com.avrix.provider.zomboid.approval;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the console approval frontend with scripted input.
 */
@DisplayName("Console approval frontend tests")
class ZomboidConsoleApprovalFrontendTest {

    @Test
    @DisplayName("Should allow, deny, and remember individual decisions")
    void shouldAllowDenyAndRemember() {
        StringWriter output = new StringWriter();
        ZomboidConsoleApprovalFrontend frontend = frontend("y\nn\ny\n", output);

        ZomboidApprovalResponse response = frontend.request(List.of(entry("a", "First"), entry("b", "Second")));

        assertThat(response.persist()).isTrue();
        assertThat(response.decisions()).containsExactly(
                new ZomboidApprovalDecision("a".repeat(64), true),
                new ZomboidApprovalDecision("b".repeat(64), false)
        );
        assertThat(output.toString()).contains("First", "Second", "allowed", "denied");
    }

    @Test
    @DisplayName("Should allow all remaining plugins")
    void shouldAllowAll() {
        StringWriter output = new StringWriter();
        ZomboidConsoleApprovalFrontend frontend = frontend("a\nn\n", output);

        ZomboidApprovalResponse response = frontend.request(List.of(entry("a", "First"), entry("b", "Second")));

        assertThat(response.persist()).isFalse();
        assertThat(response.decisions()).containsExactly(
                new ZomboidApprovalDecision("a".repeat(64), true),
                new ZomboidApprovalDecision("b".repeat(64), true)
        );
    }

    @Test
    @DisplayName("Should deny all remaining plugins")
    void shouldDenyAll() {
        StringWriter output = new StringWriter();
        ZomboidConsoleApprovalFrontend frontend = frontend("d\ny\n", output);

        ZomboidApprovalResponse response = frontend.request(List.of(entry("a", "First"), entry("b", "Second")));

        assertThat(response.persist()).isTrue();
        assertThat(response.decisions()).containsExactly(
                new ZomboidApprovalDecision("a".repeat(64), false),
                new ZomboidApprovalDecision("b".repeat(64), false)
        );
    }

    @Test
    @DisplayName("Should return an empty response when the user cancels")
    void shouldCancel() {
        StringWriter output = new StringWriter();
        ZomboidConsoleApprovalFrontend frontend = frontend("q\n", output);

        ZomboidApprovalResponse response = frontend.request(List.of(entry("a", "First")));

        assertThat(response.persist()).isFalse();
        assertThat(response.decisions()).isEmpty();
        assertThat(output.toString()).contains("cancelled");
    }

    @Test
    @DisplayName("Should return an empty response when input is closed")
    void shouldReturnEmptyOnEof() {
        StringWriter output = new StringWriter();
        ZomboidConsoleApprovalFrontend frontend = frontend("", output);

        ZomboidApprovalResponse response = frontend.request(List.of(entry("a", "First")));

        assertThat(response.persist()).isFalse();
        assertThat(response.decisions()).isEmpty();
        assertThat(output.toString()).contains("No approval input");
    }

    private static ZomboidConsoleApprovalFrontend frontend(String input, StringWriter output) {
        return new ZomboidConsoleApprovalFrontend(
                new BufferedReader(new StringReader(input)),
                new PrintWriter(output, true),
                5
        );
    }

    private static ZomboidApprovalEntry entry(String hash, String name) {
        return new ZomboidApprovalEntry(
                "test-mod",
                "test-plugin-" + hash,
                "Test Author",
                name,
                "mod",
                "Test Mod",
                "",
                Path.of("plugin-" + hash + ".jar"),
                hash.repeat(64),
                1024L,
                0L,
                "",
                List.of()
        );
    }
}
