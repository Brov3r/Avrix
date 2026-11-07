package com.avrix.packaging;

import com.avrix.core.LoaderConfig;
import com.avrix.loader.LoaderFactory;
import com.avrix.provider.zomboid.approval.ApprovalMessage;
import com.avrix.provider.zomboid.approval.ApprovalMessages;
import com.avrix.provider.zomboid.approval.ZomboidApprovalBatch;
import com.avrix.provider.zomboid.approval.ZomboidApprovalEntry;
import com.avrix.provider.zomboid.approval.ZomboidSwingApprovalMain;
import com.avrix.utils.Constants;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs isolated child JVMs against the distribution JAR, never the Gradle runtime classpath.
 */
public class PackagedRuntimeSmokeTest {

    @TempDir
    private Path directory;

    /**
     * Verifies the executable manifest and dependency wiring through the real bootstrap entrypoint.
     *
     * @throws Exception if the child JVM cannot be executed
     */
    @Test
    public void shouldRunExecutableJar() throws Exception {
        Result result = run(List.of("-D" + Constants.GAME_PROVIDER_PROPERTY + "=missing-smoke-provider",
                "-jar", loaderJar().toString()), "");
        assertThat(result.exitCode()).isEqualTo(1);
        assertThat(result.stderr()).contains("No GameProvider registered for ID [missing-smoke-provider]")
                .doesNotContain("NoClassDefFoundError", "ClassNotFoundException");
    }

    /**
     * Verifies service discovery, manifest parsing, mixin infrastructure, and translations from the packaged runtime.
     *
     * @throws Exception if fixtures or the child JVM cannot be created
     */
    @Test
    public void shouldInitializeSelfContainedRuntime() throws Exception {
        Files.createFile(directory.resolve("ProjectZomboid64.exe"));
        Path plugins = Files.createDirectories(directory.resolve("plugins"));
        try (var jar = new JarOutputStream(Files.newOutputStream(plugins.resolve("library.jar")))) {
            jar.putNextEntry(new JarEntry("metadata.yml"));
            jar.write("schema: 1\nid: smoke-library\nname: Smoke Library\nversion: 1.0.0\n"
                    .getBytes(StandardCharsets.UTF_8));
            jar.closeEntry();
        }

        // Copy only the probe class, not test services, resources, JUnit, or dependency JARs.
        Path probeRoot = directory.resolve("probe");
        String resource = RuntimeProbe.class.getName().replace('.', '/') + ".class";
        Path target = probeRoot.resolve(resource);
        Files.createDirectories(target.getParent());
        try (var input = getClass().getResourceAsStream("/" + resource)) {
            assertThat(input).isNotNull();
            Files.copy(input, target);
        }
        String classpath = loaderJar() + File.pathSeparator + probeRoot;
        Result result = run(List.of("-Davrix.game.dir=" + directory, "-Davrix.cachedir=" + directory.resolve("cache"),
                "-Davrix.approval.frontend=none", "-cp", classpath, RuntimeProbe.class.getName()), "");

        assertThat(result.exitCode()).withFailMessage(result.stdout() + result.stderr()).isZero();
        assertThat(result.stdout()).contains("PACKAGED_RUNTIME_OK");
    }

    /**
     * Verifies the real approval subprocess entrypoint using only the shaded JAR in headless mode.
     *
     * @throws Exception if the protocol or child JVM cannot be used
     */
    @Test
    public void shouldRunPackagedApprovalProcess() throws Exception {
        StringWriter request = new StringWriter();
        var entry = new ZomboidApprovalEntry("mod", "plugin", "Author", "Plugin", "mod", "Mod", "",
                directory.resolve("plugin.jar"), "a".repeat(64), 0L, 0L, "", List.of());
        ZomboidApprovalBatch.writeRequest(request, List.of(entry));
        Result result = run(List.of("-Djava.awt.headless=true", "-cp", loaderJar().toString(),
                ZomboidSwingApprovalMain.class.getName()), request.toString());

        assertThat(result.exitCode()).withFailMessage(result.stderr()).isZero();
        var response = ZomboidApprovalBatch.readResponse(new StringReader(result.stdout()));
        assertThat(response.persist()).isFalse();
        assertThat(response.decisions()).isEmpty();
    }

    private Path loaderJar() {
        return Path.of(System.getProperty("avrix.test.loader.jar")).toAbsolutePath();
    }

    private Result run(List<String> arguments, String input) throws Exception {
        String executable = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).startsWith("win")
                ? "java.exe" : "java";
        List<String> command = new ArrayList<>(List.of(
                Path.of(System.getProperty("java.home"), "bin", executable).toString(),
                "-Dfile.encoding=UTF-8", "-Dstdout.encoding=UTF-8", "-Dstderr.encoding=UTF-8",
                "-Davrix.log.dir=" + directory.resolve("logs")));
        command.addAll(arguments);
        Path stdout = directory.resolve("stdout.txt");
        Path stderr = directory.resolve("stderr.txt");
        Process process = new ProcessBuilder(command).directory(directory.toFile())
                .redirectOutput(stdout.toFile()).redirectError(stderr.toFile()).start();
        try {
            try (var writer = process.getOutputStream()) {
                writer.write(input.getBytes(StandardCharsets.UTF_8));
            }
            assertThat(process.waitFor(30, TimeUnit.SECONDS)).as("Packaged child JVM should terminate").isTrue();
            return new Result(process.exitValue(), Files.readString(stdout), Files.readString(stderr));
        } finally {
            process.destroyForcibly();
            process.waitFor(5, TimeUnit.SECONDS);
        }
    }

    private record Result(int exitCode, String stdout, String stderr) {
    }

    /**
     * Minimal child entrypoint that must run without any test or external dependency JARs.
     */
    public static final class RuntimeProbe {

        private RuntimeProbe() {
        }

        /**
         * Initializes the packaged runtime against a synthetic installation without launching game code.
         *
         * @param args unused process arguments
         */
        public static void main(String[] args) {
            if (Constants.LOADER_VERSION.contains("%")) {
                throw new IllegalStateException("Unexpanded loader version");
            }
            var messages = new ApprovalMessages();
            if (messages.get(ApprovalMessage.WINDOW_TITLE).equals(ApprovalMessage.WINDOW_TITLE.key())) {
                throw new IllegalStateException("Missing approval translations");
            }
            try (var context = LoaderFactory.create(LoaderConfig.defaults())) {
                context.initialize();
                var plugins = com.avrix.core.ServiceManager.get(com.avrix.plugins.PluginManager.class);
                if (!plugins.getPlugins().containsKey("smoke-library")) {
                    throw new IllegalStateException("Packaged plugin discovery or parsing failed");
                }
            }
            System.out.println("PACKAGED_RUNTIME_OK");
        }
    }
}
