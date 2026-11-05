package com.avrix.provider.approval;

import com.avrix.provider.ZomboidConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Launches the Swing approval window in a separate JVM.
 * <p>
 * The game runs with AWT in headless mode, so the child JVM explicitly enables AWT. The request and response are
 * exchanged over the child process pipes; no temporary files are created. Child diagnostics are drained to the
 * logger so they cannot block the protocol or corrupt standard output.
 */
public final class ZomboidSwingApprovalFrontend implements ZomboidApprovalFrontend {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZomboidSwingApprovalFrontend.class);
    private static final int DEFAULT_TIMEOUT_SECONDS = 600;
    private static final int MIN_TIMEOUT_SECONDS = 30;
    private static final int MAX_TIMEOUT_SECONDS = 3600;
    private static final int MAX_STDERR_LINES = 40;

    private final int timeoutSeconds;
    private final String mainClassName;

    /**
     * Creates a frontend using the configured approval timeout.
     */
    public ZomboidSwingApprovalFrontend() {
        this(readTimeoutSeconds(), ZomboidSwingApprovalMain.class.getName());
    }

    /**
     * Creates a frontend with an explicit timeout, primarily for tests.
     *
     * @param timeoutSeconds time to wait for user input
     */
    ZomboidSwingApprovalFrontend(int timeoutSeconds) {
        this(timeoutSeconds, ZomboidSwingApprovalMain.class.getName());
    }

    /**
     * Creates a frontend with an explicit timeout and entrypoint, primarily for tests.
     *
     * @param timeoutSeconds time to wait for user input
     * @param mainClassName  child process entrypoint
     */
    ZomboidSwingApprovalFrontend(int timeoutSeconds, String mainClassName) {
        this.timeoutSeconds = Math.max(1, Math.min(MAX_TIMEOUT_SECONDS, timeoutSeconds));
        this.mainClassName = Objects.requireNonNull(mainClassName, "Main class cannot be null");
    }

    /**
     * Shows one approval window and waits for the decisions.
     *
     * @param pending archives awaiting approval
     * @return user decisions, or an empty response when the window could not be shown
     * @throws IOException if the child process cannot be started
     */
    @Override
    public ZomboidApprovalResponse request(List<ZomboidApprovalEntry> pending) throws IOException {
        if (pending == null || pending.isEmpty()) {
            return new ZomboidApprovalResponse(false, List.of());
        }

        String classpath = locateClasspath();
        if (classpath == null) {
            LOGGER.warn("Cannot locate the Avrix JAR for the approval window; pending plugins will be skipped");
            return new ZomboidApprovalResponse(false, List.of());
        }

        ProcessBuilder builder = new ProcessBuilder(
                javaExecutable(),
                "-Djava.awt.headless=false",
                "-Dfile.encoding=UTF-8",
                "-Dstdout.encoding=UTF-8",
                "-Dstderr.encoding=UTF-8",
                "-cp",
                classpath,
                mainClassName
        );
        builder.redirectErrorStream(false);

        Process process;
        try {
            process = builder.start();
        } catch (IOException e) {
            LOGGER.error("Failed to start the approval window", e);
            throw e;
        }

        AtomicReference<ZomboidApprovalResponse> responseRef = new AtomicReference<>();
        AtomicReference<Throwable> responseError = new AtomicReference<>();
        CountDownLatch responseLatch = new CountDownLatch(1);
        List<String> stderrLines = Collections.synchronizedList(new ArrayList<>());

        Thread stdoutReader = Thread.ofVirtual().name("avrix-approval-stdout").start(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                responseRef.set(ZomboidApprovalBatch.readResponse(reader));
            } catch (Throwable throwable) {
                responseError.set(throwable);
            } finally {
                responseLatch.countDown();
            }
        });

        Thread stderrReader = Thread.ofVirtual().name("avrix-approval-stderr").start(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    synchronized (stderrLines) {
                        if (stderrLines.size() < MAX_STDERR_LINES) {
                            stderrLines.add(line);
                        }
                    }
                    LOGGER.debug("Approval UI: {}", line);
                }
            } catch (IOException e) {
                LOGGER.debug("Failed to read approval UI diagnostics", e);
            }
        });

        try {
            try (BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8))) {
                ZomboidApprovalBatch.writeRequest(writer, pending);
            }
        } catch (IOException e) {
            LOGGER.warn("Approval window closed before receiving the request; pending plugins will be skipped", e);
            process.destroyForcibly();
            return new ZomboidApprovalResponse(false, List.of());
        }

        try {
            if (!process.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
                LOGGER.warn("Approval window timed out after {} second(s); pending plugins will be skipped", timeoutSeconds);
                process.destroyForcibly();
                return new ZomboidApprovalResponse(false, List.of());
            }

            if (!responseLatch.await(2, TimeUnit.SECONDS)) {
                LOGGER.warn("Approval window closed without a response; pending plugins will be skipped");
                return new ZomboidApprovalResponse(false, List.of());
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                LOGGER.warn("Approval window exited with code {}; pending plugins will be skipped{}",
                        exitCode, stderrSummary(stderrLines));
                return new ZomboidApprovalResponse(false, List.of());
            }

            Throwable readFailure = responseError.get();
            if (readFailure != null) {
                LOGGER.warn("Approval window returned a malformed response; pending plugins will be skipped{}",
                        stderrSummary(stderrLines), readFailure);
                return new ZomboidApprovalResponse(false, List.of());
            }

            ZomboidApprovalResponse response = responseRef.get();
            if (response == null) {
                LOGGER.warn("Approval window returned no response; pending plugins will be skipped");
                return new ZomboidApprovalResponse(false, List.of());
            }
            if (!isValidResponse(pending, response)) {
                LOGGER.warn("Approval window returned decisions outside the pending batch; pending plugins will be skipped");
                return new ZomboidApprovalResponse(false, List.of());
            }
            return response;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.warn("Interrupted while waiting for plugin approval");
            return new ZomboidApprovalResponse(false, List.of());
        } finally {
            if (process.isAlive()) {
                process.destroyForcibly();
            }
            try {
                stdoutReader.join(1000);
                stderrReader.join(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private static boolean isValidResponse(
            List<ZomboidApprovalEntry> pending,
            ZomboidApprovalResponse response
    ) {
        Set<String> pendingHashes = new HashSet<>();
        for (ZomboidApprovalEntry entry : pending) {
            pendingHashes.add(entry.sha256());
        }

        Set<String> seen = new HashSet<>();
        for (ZomboidApprovalDecision decision : response.decisions()) {
            if (decision == null || decision.sha256().isBlank()
                    || !pendingHashes.contains(decision.sha256())
                    || !seen.add(decision.sha256())) {
                return false;
            }
        }
        return true;
    }

    private static String stderrSummary(List<String> lines) {
        if (lines.isEmpty()) {
            return "";
        }
        synchronized (lines) {
            return " (stderr: " + String.join(" | ", lines) + ")";
        }
    }

    private static String locateClasspath() {
        try {
            URL location = ZomboidSwingApprovalMain.class.getProtectionDomain().getCodeSource().getLocation();
            Path path = Path.of(location.toURI());
            if (Files.isRegularFile(path)) {
                return path.toAbsolutePath().toString();
            }
            if (Files.isDirectory(path)) {
                String classpath = System.getProperty("java.class.path");
                if (classpath != null && !classpath.isBlank()) {
                    return classpath;
                }
            }
        } catch (Exception e) {
            LOGGER.debug("Failed to resolve the approval window classpath", e);
        }
        String classpath = System.getProperty("java.class.path");
        return classpath == null || classpath.isBlank() ? null : classpath;
    }

    private static String javaExecutable() {
        boolean windows = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
        return Path.of(
                System.getProperty("java.home"),
                "bin",
                windows ? "java.exe" : "java"
        ).toAbsolutePath().toString();
    }

    private static int readTimeoutSeconds() {
        String value = System.getProperty(ZomboidConstants.APPROVAL_TIMEOUT_PROPERTY);
        if (value == null || value.isBlank()) {
            return DEFAULT_TIMEOUT_SECONDS;
        }
        try {
            int parsed = Integer.parseInt(value.trim());
            return Math.max(MIN_TIMEOUT_SECONDS, Math.min(MAX_TIMEOUT_SECONDS, parsed));
        } catch (NumberFormatException e) {
            LOGGER.warn("Invalid {} value [{}]; using {} seconds",
                    ZomboidConstants.APPROVAL_TIMEOUT_PROPERTY, value, DEFAULT_TIMEOUT_SECONDS);
            return DEFAULT_TIMEOUT_SECONDS;
        }
    }
}
