package com.avrix.provider.zomboid.approval;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies timeout ownership while the child JVM does not consume its input pipe.
 */
public class SwingApprovalDeadlineTest {

    @TempDir
    private Path directory;

    /**
     * Verifies that a request larger than the pipe capacity cannot bypass the timeout.
     *
     * @throws Exception if the frontend cannot start its child JVM
     */
    @Test
    @Timeout(10)
    public void shouldTimeOutWhileSendingRequest() throws Exception {
        List<ZomboidApprovalEntry> entries = IntStream.range(0, 1024).mapToObj(index ->
                new ZomboidApprovalEntry("mod", "plugin-" + index, "Author", "Plugin", "mod", "Mod", "",
                        directory.resolve("plugin.jar"), "%064x".formatted(index), 100L, 0L, "", List.of())).toList();
        var frontend = new ZomboidSwingApprovalFrontend(1, NonReadingChild.class.getName());
        long started = System.nanoTime();

        assertThat(frontend.request(entries).decisions()).isEmpty();

        assertThat(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started)).isLessThan(5000L);
        assertThat(ProcessHandle.current().descendants().filter(ProcessHandle::isAlive)
                .flatMap(child -> java.util.Arrays.stream(child.info().arguments().orElse(new String[0]))))
                .doesNotContain(NonReadingChild.class.getName());
    }

    /**
     * Child JVM that leaves stdin unread for longer than the configured frontend timeout.
     */
    public static final class NonReadingChild {

        private NonReadingChild() {
        }

        /**
         * Waits without consuming the request.
         *
         * @param args unused process arguments
         * @throws InterruptedException if the sleep is interrupted
         */
        public static void main(String[] args) throws InterruptedException {
            Thread.sleep(30_000L);
        }
    }
}
