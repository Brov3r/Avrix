package com.avrix.provider.zomboid;

import com.avrix.api.PluginApprovalGate;
import com.avrix.api.PluginArchive;
import com.avrix.core.Environment;
import com.avrix.core.Metadata;
import com.avrix.provider.zomboid.approval.ZomboidApprovalDecision;
import com.avrix.provider.zomboid.approval.ZomboidApprovalResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies approval-store merging across stale snapshots, threads, and independent JVMs.
 */
public class ApprovalStoreConcurrencyTest {

    @TempDir
    private Path directory;

    /**
     * Verifies that saving from a stale gate does not discard another gate's decision.
     *
     * @throws Exception if fixture files cannot be written or read
     */
    @Test
    public void shouldMergeStaleSnapshots() throws Exception {
        var first = gate(directory, true);
        var second = gate(directory, false);
        queue(first, directory, 1);
        queue(second, directory, 2);
        first.flushPending();
        second.flushPending();
        assertSaved(directory, 2);
    }

    /**
     * Verifies that overlapping same-JVM writers do not fail with overlapping file locks.
     *
     * @throws Exception if concurrent work or fixture IO fails
     */
    @Test
    @Timeout(20)
    public void shouldSerializeLocalWriters() throws Exception {
        List<ZomboidPluginApprovalGate> gates = new ArrayList<>();
        for (int index = 1; index <= 8; index++) {
            var gate = gate(directory, index % 2 == 1);
            queue(gate, directory, index);
            gates.add(gate);
        }
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(gates.size())) {
            var futures = gates.stream().map(gate -> executor.submit(() -> {
                start.await();
                gate.flushPending();
                return null;
            })).toList();
            start.countDown();
            for (var future : futures) {
                future.get(10, TimeUnit.SECONDS);
            }
        }
        assertSaved(directory, gates.size());
    }

    /**
     * Verifies merging when separate JVMs load the initial snapshot before either saves.
     *
     * @throws Exception if the child JVMs or fixture IO fail
     */
    @Test
    @Timeout(30)
    public void shouldMergeIndependentProcesses() throws Exception {
        List<Process> children = new ArrayList<>();
        try {
            for (int index = 1; index <= 2; index++) {
                String executable = System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java";
                children.add(new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", executable).toString(),
                        "-Davrix.log.dir=" + directory.resolve("logs-" + index), "-cp", childClasspath(),
                        SaveChild.class.getName(), directory.toString(), Integer.toString(index))
                        .redirectErrorStream(true).redirectOutput(directory.resolve("child-" + index + ".txt").toFile())
                        .start());
            }
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(15);
            while ((!Files.exists(directory.resolve("ready-1")) || !Files.exists(directory.resolve("ready-2")))
                    && System.nanoTime() < deadline) {
                Thread.sleep(10);
            }
            assertThat(directory.resolve("ready-1")).isRegularFile();
            assertThat(directory.resolve("ready-2")).isRegularFile();
            Files.createFile(directory.resolve("save"));
            for (int index = 0; index < children.size(); index++) {
                Process child = children.get(index);
                assertThat(child.waitFor(10, TimeUnit.SECONDS)).isTrue();
                assertThat(child.exitValue()).withFailMessage(Files.readString(directory.resolve("child-" + (index + 1) + ".txt")))
                        .isZero();
            }
            assertSaved(directory, 2);
        } finally {
            for (Process child : children) {
                child.destroyForcibly();
                child.waitFor(5, TimeUnit.SECONDS);
            }
        }
    }

    private static ZomboidPluginApprovalGate gate(Path root, boolean allow) {
        return new ZomboidPluginApprovalGate(root, Environment.CLIENT, "prompt", entries ->
                new ZomboidApprovalResponse(true, entries.stream()
                        .map(entry -> new ZomboidApprovalDecision(entry.sha256(), allow)).toList()));
    }

    private static void queue(ZomboidPluginApprovalGate gate, Path root, int index) throws Exception {
        Path archive = Files.writeString(root.resolve("plugin-" + index + ".jar"), "plugin-" + index);
        gate.evaluate(new PluginArchive(archive.toFile(), "mod-" + index, "Mod", "mod", Map.of(), false),
                new Metadata.Builder().id("plugin-" + index).name("Plugin").version("1.0.0").build(), hash(index));
    }

    private static String hash(int index) {
        return "%064x".formatted(index);
    }

    private static void assertSaved(Path root, int count) throws Exception {
        String saved = Files.readString(root.resolve("plugin-approvals.txt"));
        for (int index = 1; index <= count; index++) {
            assertThat(saved).contains("APPROVAL\t" + hash(index) + "\t" + (index % 2 == 1) + "\t");
        }
        try (var files = Files.list(root)) {
            assertThat(files.filter(path -> path.toString().endsWith(".tmp"))).isEmpty();
        }
        var reload = new ZomboidPluginApprovalGate(root, Environment.CLIENT, "deny-new", null);
        for (int index = 1; index <= count; index++) {
            var archive = new PluginArchive(root.resolve("plugin-" + index + ".jar").toFile(), "mod", "Mod", "mod", Map.of(), false);
            assertThat(reload.evaluate(archive, null, hash(index)))
                    .isEqualTo(index % 2 == 1 ? PluginApprovalGate.Status.APPROVED : PluginApprovalGate.Status.DENIED);
        }
    }

    private String childClasspath() throws Exception {
        var paths = new LinkedHashSet<String>();
        for (ClassLoader loader = getClass().getClassLoader(); loader != null; loader = loader.getParent()) {
            if (loader instanceof URLClassLoader urls) {
                for (var url : urls.getURLs()) {
                    if ("file".equals(url.getProtocol())) {
                        paths.add(Path.of(url.toURI()).toAbsolutePath().toString());
                    }
                }
            }
        }
        Arrays.stream(System.getProperty("java.class.path").split(Pattern.quote(File.pathSeparator)))
                .map(path -> Path.of(path).toAbsolutePath().toString()).forEach(paths::add);
        return String.join(File.pathSeparator, paths);
    }

    /**
     * Independent store writer used to exercise the operating system file lock.
     */
    public static final class SaveChild {

        private SaveChild() {
        }

        /**
         * Loads a gate, signals readiness, and saves after the parent releases both processes.
         *
         * @param args store directory and writer index
         * @throws Exception if coordination or store IO fails
         */
        public static void main(String[] args) throws Exception {
            Path root = Path.of(args[0]);
            int index = Integer.parseInt(args[1]);
            var gate = gate(root, index % 2 == 1);
            queue(gate, root, index);
            Files.createFile(root.resolve("ready-" + index));
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(20);
            while (!Files.exists(root.resolve("save"))) {
                if (System.nanoTime() >= deadline) {
                    throw new IllegalStateException("Parent did not release the store writers");
                }
                Thread.sleep(10);
            }
            gate.flushPending();
        }
    }
}
