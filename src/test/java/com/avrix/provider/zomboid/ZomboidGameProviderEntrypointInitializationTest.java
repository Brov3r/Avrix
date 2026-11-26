package com.avrix.provider.zomboid;

import com.avrix.core.KnotClassLoader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that the game entrypoint is initialized outside the provider launch monitor.
 */
@DisplayName("ZomboidGameProvider entrypoint initialization tests")
class ZomboidGameProviderEntrypointInitializationTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("Should not initialize the entrypoint while holding the launch monitor")
    void shouldInitializeEntrypointOutsideLaunchMonitor() throws Exception {
        Path gameRoot = Files.createDirectories(tempDir.resolve("game"));
        Files.createFile(gameRoot.resolve("ProjectZomboid64.exe"));

        ProcessBuilder builder = new ProcessBuilder(
                javaExecutable().toString(),
                "-Davrix.game.dir=" + gameRoot,
                "-cp",
                childClasspath(),
                ChildMain.class.getName()
        );
        builder.directory(gameRoot.toFile());
        builder.redirectErrorStream(true);
        Process process = builder.start();
        try {
            boolean finished = process.waitFor(30, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
            }
            assertThat(finished).isTrue();
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            assertThat(process.exitValue()).as(output).isZero();
            assertThat(output).contains("heldMonitor=false", "entrypointRan=true");
        } finally {
            process.destroyForcibly();
        }
    }

    private static Path javaExecutable() {
        boolean windows = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
        return Path.of(System.getProperty("java.home"), "bin", windows ? "java.exe" : "java");
    }

    private static String childClasspath() {
        StringBuilder classpath = new StringBuilder();
        for (String entry : System.getProperty("java.class.path").split(File.pathSeparator)) {
            if (classpath.length() > 0) {
                classpath.append(File.pathSeparator);
            }
            classpath.append(Path.of(entry).toAbsolutePath());
        }
        return classpath.toString();
    }

    /**
     * Child JVM that launches a synthetic entrypoint through the real provider.
     */
    public static final class ChildMain {
        private ChildMain() {
        }

        /**
         * Runs the synthetic launch.
         *
         * @param args unused process arguments
         * @throws Exception if provider setup fails
         */
        public static void main(String[] args) throws Exception {
            try (KnotClassLoader loader = new KnotClassLoader(new URL[0], ChildMain.class.getClassLoader())) {
                ZomboidGameProvider provider = new ZomboidGameProvider() {
                    /** {@inheritDoc} */
                    @Override
                    public String getEntrypoint() {
                        return MonitorProbeEntrypoint.class.getName();
                    }

                    /** {@inheritDoc} */
                    @Override
                    public String getNormalizedVersion() {
                        return "42.0.0";
                    }

                    /** {@inheritDoc} */
                    @Override
                    public List<Path> getJavaLibsPath() {
                        return List.of();
                    }
                };
                ProbeState.provider = provider;
                try {
                    provider.init(loader);
                    provider.launch(new String[0]);
                    System.out.println("heldMonitor=" + ProbeState.heldMonitor);
                    System.out.println("entrypointRan=" + ProbeState.entrypointRan);
                } finally {
                    provider.close();
                }
            }
        }
    }

    /**
     * State shared between the synthetic entrypoint and its child process.
     */
    public static final class ProbeState {
        private static ZomboidGameProvider provider;
        private static boolean heldMonitor;
        private static boolean entrypointRan;

        private ProbeState() {
        }
    }

    /**
     * Synthetic entrypoint that records whether class initialization held the provider monitor.
     */
    public static final class MonitorProbeEntrypoint {
        static {
            ProbeState.heldMonitor = Thread.holdsLock(ProbeState.provider);
        }

        private MonitorProbeEntrypoint() {
        }

        /**
         * Marks that the entrypoint itself ran.
         *
         * @param args unused game arguments
         */
        public static void main(String[] args) {
            ProbeState.entrypointRan = true;
        }
    }
}
