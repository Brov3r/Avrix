package com.avrix;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;

/**
 * Branch-focused unit tests for {@link Main} reflection-safe behavior.
 * <p>
 * {@link Main#main(String[])} is intentionally never invoked because it starts the full game runtime.
 */
@DisplayName("Main Branch Tests")
class MainBranchesTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("Should prevent direct instantiation of the entrypoint class")
    void shouldPreventDirectInstantiation() throws Exception {
        Constructor<Main> constructor = Main.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        assertThatThrownBy(constructor::newInstance)
                .hasCauseInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("Should strip repeated loader flags while preserving game argument order")
    void shouldStripRepeatedLoaderFlagsAndPreserveOrder() throws Exception {
        Method filter = Main.class.getDeclaredMethod("filterLoaderArguments", String[].class);
        filter.setAccessible(true);

        String[] result = (String[]) filter.invoke(
                null,
                (Object) new String[]{
                        "-game",
                        "--strict",
                        "--debug",
                        "--no-redirect-log",
                        "--strict",
                        "-more"
                }
        );

        assertThat(result).containsExactly("-game", "--debug", "-more");
    }

    @Test
    @DisplayName("Should not initialize logging or run retention when Main is only loaded")
    void shouldNotTouchLogsWhenMainClassIsLoaded() throws Exception {
        Path logDirectory = Files.createDirectories(tempDir.resolve("logs"));
        Path oldLog = Files.writeString(logDirectory.resolve("avrix-old.log"), "old");
        Files.setLastModifiedTime(oldLog, FileTime.from(Instant.now().minus(10, ChronoUnit.DAYS)));

        Path processOutput = tempDir.resolve("main-access-probe.log");
        Process process = new ProcessBuilder(
                javaExecutable(),
                "-Davrix.log.dir=" + logDirectory,
                "-cp",
                childClasspath(),
                MainAccessProbe.class.getName()
        ).redirectErrorStream(true)
                .redirectOutput(processOutput.toFile())
                .start();

        boolean finished = process.waitFor(30, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            fail("Main class access probe did not finish within 30 seconds");
        }

        String output = Files.exists(processOutput)
                ? Files.readString(processOutput)
                : "";
        assertThat(process.exitValue())
                .as("Main class access probe output:%n%s", output)
                .isZero();

        try (var files = Files.list(logDirectory)) {
            List<String> names = files.map(path -> path.getFileName().toString()).toList();
            assertThat(names).containsExactly("avrix-old.log");
        }
    }

    private static String javaExecutable() {
        boolean windows = System.getProperty("os.name", "")
                .toLowerCase(Locale.ROOT)
                .contains("win");
        return Path.of(System.getProperty("java.home"), "bin", windows ? "java.exe" : "java").toString();
    }

    private static String childClasspath() {
        Set<String> entries = new LinkedHashSet<>();
        String classpath = System.getProperty("java.class.path");
        if (classpath != null) {
            entries.addAll(Arrays.asList(classpath.split(File.pathSeparator)));
        }

        ClassLoader loader = Main.class.getClassLoader();
        while (loader != null) {
            if (loader instanceof URLClassLoader urlClassLoader) {
                for (URL url : urlClassLoader.getURLs()) {
                    addCodeSource(entries, url);
                }
            }
            loader = loader.getParent();
        }

        addCodeSource(entries, Main.class);
        addCodeSource(entries, MainAccessProbe.class);
        return String.join(File.pathSeparator, entries);
    }

    private static void addCodeSource(Set<String> entries, Class<?> type) {
        var codeSource = type.getProtectionDomain().getCodeSource();
        if (codeSource != null) {
            addCodeSource(entries, codeSource.getLocation());
        }
    }

    private static void addCodeSource(Set<String> entries, URL url) {
        if (url == null || !"file".equalsIgnoreCase(url.getProtocol())) {
            return;
        }
        try {
            entries.add(Path.of(url.toURI()).toString());
        } catch (Exception ignored) {
            // A malformed classpath entry cannot be forwarded to the child JVM.
        }
    }

    /**
     * Loads {@link Main} without invoking its entrypoint.
     */
    public static final class MainAccessProbe {

        private MainAccessProbe() {
        }

        /**
         * Class-loading probe entrypoint.
         *
         * @param args unused
         * @throws ClassNotFoundException if Main cannot be loaded
         */
        public static void main(String[] args) throws ClassNotFoundException {
            Class.forName(Main.class.getName(), true, Main.class.getClassLoader());
        }
    }
}
