package com.avrix.provider.zomboid.approval;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.awt.GraphicsEnvironment;
import java.io.OutputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * Integration tests for the standalone approval entrypoint under headless and bundled-JRE conditions.
 * <p>
 * Bundled-JRE tests require {@code -Davrix.test.game.dir}; the GUI test additionally requires
 * {@code -Davrix.test.approval.ui=true} and a non-headless desktop.
 */
class ZomboidSwingApprovalMainTest {

    @Test
    void shouldReturnEmptyResponseWhenHeadless() throws Exception {
        ProcessResult result = run(javaExecutable(Path.of(System.getProperty("java.home"))), request());

        assertThat(result.exitCode()).isZero();
        assertThat(result.stdout())
                .contains("RESP\t1", "PERSIST\t0")
                .doesNotContain("DECISION");
    }

    @Test
    void shouldRunWithBundledGameJre() throws Exception {
        Path gameJava = gameJreExecutable();

        ProcessResult result = run(gameJava, request());

        assertThat(result.exitCode()).isZero();
        assertThat(result.stdout()).contains("RESP\t1", "PERSIST\t0");
    }

    @Test
    void shouldStartWindowWithBundledGameJre() throws Exception {
        Assumptions.assumeTrue(
                Boolean.getBoolean("avrix.test.approval.ui"),
                "Approval UI test is opt-in; pass -Davrix.test.approval.ui=true"
        );
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "No desktop is available");
        Path gameJava = gameJreExecutable();

        Process process = start(gameJava, "-Djava.awt.headless=false", request());
        try {
            assertThat(process.waitFor(1500, TimeUnit.MILLISECONDS))
                    .as("Approval window process should stay alive")
                    .isFalse();
        } finally {
            process.destroyForcibly();
            process.waitFor(10, TimeUnit.SECONDS);
        }
    }

    private static ProcessResult run(Path javaExecutable, String request) throws Exception {
        Process process = start(javaExecutable, "-Djava.awt.headless=true", request);

        if (!process.waitFor(20, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            fail("Approval entrypoint did not finish within 20 seconds");
        }

        String stdout = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        String stderr = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
        return new ProcessResult(process.exitValue(), stdout, stderr);
    }

    private static Process start(Path javaExecutable, String headless, String request) throws Exception {
        ProcessBuilder builder = new ProcessBuilder(
                javaExecutable.toString(),
                headless,
                "-Dfile.encoding=UTF-8",
                "-Dstdout.encoding=UTF-8",
                "-Dstderr.encoding=UTF-8",
                "-cp",
                System.getProperty("java.class.path"),
                ZomboidSwingApprovalMain.class.getName()
        );

        Process process = builder.start();
        try (OutputStream input = process.getOutputStream()) {
            input.write(request.getBytes(StandardCharsets.UTF_8));
            input.flush();
        }
        return process;
    }

    private static Path gameJreExecutable() {
        String gameDir = System.getProperty("avrix.test.game.dir");
        Assumptions.assumeTrue(
                gameDir != null && !gameDir.isBlank(),
                "Set -Davrix.test.game.dir to the Project Zomboid installation directory"
        );
        Path gameJava = javaExecutable(Path.of(gameDir).resolve("jre64"));
        Assumptions.assumeTrue(
                Files.isRegularFile(gameJava),
                "Project Zomboid bundled JRE not available: " + gameJava
        );
        return gameJava;
    }

    private static String request() throws Exception {
        ZomboidApprovalEntry entry = new ZomboidApprovalEntry(
                "test-mod",
                "test-plugin",
                "Test Author",
                "Test Plugin",
                "mod",
                "Test Mod",
                "",
                Path.of(System.getProperty("java.io.tmpdir"), "approval-test.jar"),
                "a".repeat(64),
                1024L,
                0L,
                "",
                List.of("test.mixin.SampleMixin")
        );
        StringWriter writer = new StringWriter();
        ZomboidApprovalBatch.writeRequest(writer, List.of(entry));
        return writer.toString();
    }

    private static Path javaExecutable(Path javaHome) {
        boolean windows = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
        return javaHome.resolve("bin").resolve(windows ? "java.exe" : "java");
    }

    private record ProcessResult(int exitCode, String stdout, String stderr) {
    }
}
