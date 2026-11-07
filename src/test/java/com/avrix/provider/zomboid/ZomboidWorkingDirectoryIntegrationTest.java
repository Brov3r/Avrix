package com.avrix.provider.zomboid;

import com.avrix.core.KnotClassLoader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileInputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/** Verifies working-directory handling in a real child JVM without game files. */
public class ZomboidWorkingDirectoryIntegrationTest {

    @TempDir
    private Path tempDir;

    /**
     * Verifies that a JVM started in the game root resolves both Java IO and NIO relative paths correctly.
     *
     * @throws Exception if fixture creation or the child process fails
     */
    @Test
    public void shouldLaunchFromActualGameRoot() throws Exception {
        Path gameRoot = createInstallation();
        Path output = tempDir.resolve("success.txt");
        assertThat(runProbe(gameRoot, gameRoot, output)).isZero();
        assertThat(Files.readString(output)).contains("NIO=fixture-media", "IO=fixture-media");
        assertThat(gameRoot.resolve(ZomboidConstants.RUNTIME_MARKER_NAME)).doesNotExist();
    }

    /**
     * Verifies that selecting an installation does not disguise an incorrect process working directory.
     *
     * @throws Exception if fixture creation or the child process fails
     */
    @Test
    public void shouldRejectDifferentWorkingDirectoryBeforeGameStartup() throws Exception {
        Path gameRoot = createInstallation();
        Path otherDirectory = Files.createDirectories(tempDir.resolve("other"));
        Path output = tempDir.resolve("failure.txt");
        assertThat(runProbe(gameRoot, otherDirectory, output)).isNotZero();
        assertThat(Files.readString(output)).contains("Change directory to the game root").doesNotContain("NIO=");
        assertThat(gameRoot.resolve(ZomboidConstants.RUNTIME_MARKER_NAME)).doesNotExist();
    }

    private Path createInstallation() throws Exception {
        Path gameRoot = Files.createDirectories(tempDir.resolve("game"));
        Files.createFile(gameRoot.resolve("ProjectZomboid64.exe"));
        Files.writeString(gameRoot.resolve("media.txt"), "fixture-media");
        return gameRoot;
    }

    private int runProbe(Path gameRoot, Path workingDirectory, Path output) throws Exception {
        String executable = System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java";
        Process process = new ProcessBuilder(
                Path.of(System.getProperty("java.home"), "bin", executable).toString(),
                "-Davrix.game.dir=" + gameRoot,
                 "-Davrix.log.dir=" + tempDir.resolve("logs"),
                 "-D" + ZomboidConstants.CACHE_DIR_PROPERTY + "=" + tempDir.resolve("cache"),
                "-cp", childClasspath(), ChildMain.class.getName()
        ).directory(workingDirectory.toFile()).redirectErrorStream(true).redirectOutput(output.toFile()).start();
        try {
            assertThat(process.waitFor(20, TimeUnit.SECONDS)).isTrue();
            return process.exitValue();
        } finally {
            process.destroyForcibly();
        }
    }

    private String childClasspath() throws Exception {
        var paths = new LinkedHashSet<String>();
        for (ClassLoader loader = getClass().getClassLoader(); loader != null; loader = loader.getParent()) {
            if (loader instanceof URLClassLoader urls) {
                for (URL url : urls.getURLs()) {
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

    /** Child JVM entrypoint that launches a synthetic game through the real provider. */
    public static final class ChildMain {
        private ChildMain() {
        }

        /**
         * Initializes the provider and invokes a synthetic game entrypoint.
         *
         * @param args unused process arguments
         * @throws Exception if provider setup or shutdown fails
         */
        public static void main(String[] args) throws Exception {
            try (var loader = new KnotClassLoader(new URL[0], ChildMain.class.getClassLoader())) {
                var provider = new ZomboidGameProvider() {
                    /** {@inheritDoc} */
                    @Override
                    public String getEntrypoint() {
                        return GameMain.class.getName();
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
                try {
                    provider.init(loader);
                    provider.beforeLaunch();
                    provider.launch(args);
                } finally {
                    provider.close();
                }
            }
        }
    }

    /** Synthetic game entrypoint that reads a relative media path. */
    public static final class GameMain {
        private GameMain() {
        }

        /**
         * Reads fixture media through both filesystem APIs.
         *
         * @param args unused game arguments
         * @throws Exception if relative media lookup fails
         */
        public static void main(String[] args) throws Exception {
            System.out.println("NIO=" + Files.readString(Path.of("media.txt")));
            try (var input = new FileInputStream("media.txt")) {
                System.out.println("IO=" + new String(input.readAllBytes(), StandardCharsets.UTF_8));
            }
        }
    }
}
