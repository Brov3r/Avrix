package com.avrix.provider.approval;

import com.avrix.api.PluginArchive;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for the stdin/stdout approval protocol.
 */
@DisplayName("Zomboid approval batch protocol")
class ZomboidApprovalBatchTest {

    @Test
    @DisplayName("Should round-trip approval requests with names, paths, and hashes")
    void shouldRoundTripRequest() throws IOException {
        List<ZomboidApprovalEntry> entries = List.of(new ZomboidApprovalEntry(
                "mod\twith\ttabs",
                "plugin-ß",
                "Test Author",
                "Мод с пробелами",
                PluginArchive.SOURCE_MOD,
                "Local mod",
                "",
                Path.of("C:/Games/Project Zomboid/mods/My Mod/media/java/plugin.jar"),
                "a".repeat(64),
                123456L,
                1700000000000L,
                "b".repeat(64)
        ));

        StringWriter writer = new StringWriter();
        ZomboidApprovalBatch.writeRequest(writer, entries);

        List<ZomboidApprovalEntry> parsed = ZomboidApprovalBatch.readRequest(
                new StringReader(writer.toString())
        );

        assertThat(parsed).containsExactlyElementsOf(entries);
    }

    @Test
    @DisplayName("Should round-trip approval responses")
    void shouldRoundTripResponse() throws IOException {
        ZomboidApprovalResponse response = new ZomboidApprovalResponse(true, List.of(
                new ZomboidApprovalDecision("a".repeat(64), true),
                new ZomboidApprovalDecision("b".repeat(64), false)
        ));

        StringWriter writer = new StringWriter();
        ZomboidApprovalBatch.writeResponse(writer, response);

        ZomboidApprovalResponse parsed = ZomboidApprovalBatch.readResponse(
                new StringReader(writer.toString())
        );

        assertThat(parsed).isEqualTo(response);
    }

    @Test
    @DisplayName("Should round-trip an empty request and response")
    void shouldRoundTripEmptyBatch() throws IOException {
        StringWriter requestWriter = new StringWriter();
        ZomboidApprovalBatch.writeRequest(requestWriter, List.of());
        assertThat(ZomboidApprovalBatch.readRequest(new StringReader(requestWriter.toString()))).isEmpty();

        StringWriter responseWriter = new StringWriter();
        ZomboidApprovalBatch.writeResponse(responseWriter, new ZomboidApprovalResponse(false, List.of()));
        ZomboidApprovalResponse response = ZomboidApprovalBatch.readResponse(new StringReader(responseWriter.toString()));
        assertThat(response.persist()).isFalse();
        assertThat(response.decisions()).isEmpty();
    }

    @Test
    @DisplayName("Should reject a request with a bad header")
    void shouldRejectBadRequestHeader() {
        assertThatThrownBy(() -> ZomboidApprovalBatch.readRequest(new StringReader("BAD\nEND\n")))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("header");
    }

    @Test
    @DisplayName("Should reject a truncated request")
    void shouldRejectTruncatedRequest() {
        assertThatThrownBy(() -> ZomboidApprovalBatch.readRequest(new StringReader("REQ\t1\n")))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("END");
    }

    @Test
    @DisplayName("Should reject a malformed entry")
    void shouldRejectMalformedEntry() {
        assertThatThrownBy(() -> ZomboidApprovalBatch.readRequest(new StringReader("REQ\t1\nENTRY\tonly-one-field\nEND\n")))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("Malformed");
    }

    @Test
    @DisplayName("Should reject a response with duplicate PERSIST")
    void shouldRejectDuplicatePersist() {
        assertThatThrownBy(() -> ZomboidApprovalBatch.readResponse(new StringReader(
                "RESP\t1\nPERSIST\t1\nPERSIST\t0\nEND\n"
        ))).isInstanceOf(IOException.class).hasMessageContaining("Duplicate");
    }

    @Test
    @DisplayName("Should reject a response with an invalid boolean")
    void shouldRejectInvalidBoolean() {
        assertThatThrownBy(() -> ZomboidApprovalBatch.readResponse(new StringReader(
                "RESP\t1\nPERSIST\tmaybe\nEND\n"
        ))).isInstanceOf(IOException.class).hasMessageContaining("boolean");
    }
}
