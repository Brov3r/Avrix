package com.avrix.provider.zomboid;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that the runtime marker is removed when the loader process is killed.
 */
@EnabledOnOs(OS.WINDOWS)
public class RuntimeMarkerCrashTest {

    @TempDir
    private Path directory;

    /**
     * Verifies that a forcibly terminated loader process does not leave the runtime marker behind.
     *
     * @throws Exception if the child process cannot be started or stopped
     */
    @Test
    public void shouldRemoveMarkerAfterForcedTermination() throws Exception {
        Path game = Files.createDirectories(directory.resolve("game"));
        Path cache = Files.createDirectories(directory.resolve("cache"));
        Path marker = cache.resolve("Lua").resolve(ZomboidConstants.RUNTIME_MARKER_NAME);
        Path java = Path.of(System.getProperty("java.home"), "bin", "java.exe");

        Process process = new ProcessBuilder(
                java.toString(),
                "-cp", System.getProperty("java.class.path"),
                Holder.class.getName(),
                game.toString(),
                cache.toString()
        ).redirectErrorStream(true).start();

        try {
            await(marker, true, 20);
            assertThat(marker).isRegularFile();
            assertThat(process.isAlive()).isTrue();
        } finally {
            process.destroyForcibly();
            assertThat(process.waitFor(20, TimeUnit.SECONDS)).isTrue();
        }

        await(marker, false, 10);
        assertThat(marker).doesNotExist();
    }

    private static void await(Path file, boolean present, long timeoutSeconds) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(timeoutSeconds);
        while (Files.exists(file) != present && System.nanoTime() < deadline) {
            Thread.sleep(50);
        }
    }

    /**
     * Child process entrypoint that holds the runtime marker open until it is killed.
     */
    public static final class Holder {

        /**
         * Prevents instantiation of the child process helper.
         *
         * @throws UnsupportedOperationException always
         */
        private Holder() {
            throw new UnsupportedOperationException("This is a helper class and cannot be instantiated.");
        }

        /**
         * Creates the runtime marker and waits until the parent terminates the process.
         *
         * @param args game directory followed by the cache directory
         * @throws Exception if the marker cannot be created
         */
        public static void main(String[] args) throws Exception {
            System.setProperty("user.dir", args[0]);
            try (var marker = new ZomboidRuntimeMarker()) {
                marker.activate(Path.of(args[1]));
                System.out.println("READY");
                System.out.flush();
                Thread.sleep(TimeUnit.MINUTES.toMillis(5));
            }
        }
    }
}
