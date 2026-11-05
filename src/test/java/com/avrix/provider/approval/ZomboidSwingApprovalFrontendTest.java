package com.avrix.provider.approval;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the child-process lifecycle of the Swing approval frontend using lightweight test entrypoints.
 */
@DisplayName("Swing approval frontend process tests")
class ZomboidSwingApprovalFrontendTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("Should return decisions from the child process without temporary files")
    void shouldReturnChildDecisions() throws Exception {
        ZomboidApprovalEntry entry = entry("a".repeat(64));
        ZomboidSwingApprovalFrontend frontend = new ZomboidSwingApprovalFrontend(
                10,
                AllowAllChild.class.getName()
        );

        ZomboidApprovalResponse response = frontend.request(List.of(entry));

        assertThat(response.persist()).isTrue();
        assertThat(response.decisions())
                .containsExactly(new ZomboidApprovalDecision(entry.sha256(), true));
    }

    @Test
    @DisplayName("Should reject decisions for hashes outside the request")
    void shouldRejectUnknownDecision() throws Exception {
        ZomboidSwingApprovalFrontend frontend = new ZomboidSwingApprovalFrontend(
                10,
                UnknownDecisionChild.class.getName()
        );

        ZomboidApprovalResponse response = frontend.request(List.of(entry("a".repeat(64))));

        assertThat(response.decisions()).isEmpty();
        assertThat(response.persist()).isFalse();
    }

    @Test
    @DisplayName("Should time out and destroy a hanging child process")
    void shouldTimeOutHangingChild() throws Exception {
        ZomboidSwingApprovalFrontend frontend = new ZomboidSwingApprovalFrontend(
                1,
                SleepingChild.class.getName()
        );

        long start = System.nanoTime();
        ZomboidApprovalResponse response = frontend.request(List.of(entry("a".repeat(64))));
        long elapsedMillis = (System.nanoTime() - start) / 1_000_000L;

        assertThat(response.decisions()).isEmpty();
        assertThat(elapsedMillis).isLessThan(10_000L);
    }

    @Test
    @DisplayName("Should treat a cancelled child as an empty response")
    void shouldTreatCancelAsEmpty() throws Exception {
        ZomboidSwingApprovalFrontend frontend = new ZomboidSwingApprovalFrontend(
                10,
                CancelChild.class.getName()
        );

        ZomboidApprovalResponse response = frontend.request(List.of(entry("a".repeat(64))));

        assertThat(response.decisions()).isEmpty();
    }

    @Test
    @DisplayName("Should not start a child process for an empty batch")
    void shouldSkipEmptyBatch() throws Exception {
        ZomboidSwingApprovalFrontend frontend = new ZomboidSwingApprovalFrontend(
                1,
                SleepingChild.class.getName()
        );

        assertThat(frontend.request(List.of()).decisions()).isEmpty();
    }

    private ZomboidApprovalEntry entry(String hash) {
        return new ZomboidApprovalEntry(
                "test-mod",
                "test-plugin",
                "Test Author",
                "Test Plugin",
                "mod",
                "Test Mod",
                "",
                tempDir.resolve("plugin.jar"),
                hash,
                100L,
                0L,
                ""
        );
    }

    /**
     * Test child that approves every pending entry.
     */
    public static final class AllowAllChild {
        private AllowAllChild() {
        }

        /**
         * Runs the test child.
         *
         * @param args unused
         * @throws Exception if the protocol fails
         */
        public static void main(String[] args) throws Exception {
            List<ZomboidApprovalEntry> entries = ZomboidApprovalBatch.readRequest(
                    new InputStreamReader(System.in, StandardCharsets.UTF_8)
            );
            System.err.println("test child diagnostic line");
            ZomboidApprovalBatch.writeResponse(
                    new OutputStreamWriter(System.out, StandardCharsets.UTF_8),
                    new ZomboidApprovalResponse(true, entries.stream()
                            .map(entry -> new ZomboidApprovalDecision(entry.sha256(), true))
                            .toList())
            );
        }
    }

    /**
     * Test child that returns a decision for an unknown hash.
     */
    public static final class UnknownDecisionChild {
        private UnknownDecisionChild() {
        }

        /**
         * Runs the test child.
         *
         * @param args unused
         * @throws Exception if the protocol fails
         */
        public static void main(String[] args) throws Exception {
            ZomboidApprovalBatch.readRequest(new InputStreamReader(System.in, StandardCharsets.UTF_8));
            ZomboidApprovalBatch.writeResponse(
                    new OutputStreamWriter(System.out, StandardCharsets.UTF_8),
                    new ZomboidApprovalResponse(false, List.of(
                            new ZomboidApprovalDecision("deadbeef", true)
                    ))
            );
        }
    }

    /**
     * Test child that never answers.
     */
    public static final class SleepingChild {
        private SleepingChild() {
        }

        /**
         * Runs the test child.
         *
         * @param args unused
         * @throws Exception if interrupted
         */
        public static void main(String[] args) throws Exception {
            ZomboidApprovalBatch.readRequest(new InputStreamReader(System.in, StandardCharsets.UTF_8));
            Thread.sleep(30_000L);
        }
    }

    /**
     * Test child that exits as if the user cancelled.
     */
    public static final class CancelChild {
        private CancelChild() {
        }

        /**
         * Runs the test child.
         *
         * @param args unused
         */
        public static void main(String[] args) {
            System.exit(2);
        }
    }
}
