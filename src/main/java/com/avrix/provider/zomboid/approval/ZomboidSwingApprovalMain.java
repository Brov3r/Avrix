package com.avrix.provider.zomboid.approval;

import com.avrix.provider.zomboid.approval.ui.ApprovalDialog;
import com.avrix.provider.zomboid.approval.ui.ZomboidApprovalTheme;
import com.avrix.utils.FatalErrors;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.GraphicsEnvironment;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Standalone Swing entry point for the Project Zomboid approval window.
 * <p>
 * The process reads one {@link ZomboidApprovalBatch} request from standard input and writes one response to standard
 * output. All diagnostics go to standard error so the pipe protocol is never corrupted.
 */
public final class ZomboidSwingApprovalMain {

    private ZomboidSwingApprovalMain() {
        throw new UnsupportedOperationException("UI entrypoint class cannot be instantiated");
    }

    /**
     * Runs the approval window for one request.
     *
     * @param args unused
     */
    public static void main(String[] args) {
        PrintStream protocol = new PrintStream(new FileOutputStream(FileDescriptor.out), true, StandardCharsets.UTF_8);
        System.setOut(System.err);

        try {
            List<ZomboidApprovalEntry> entries = ZomboidApprovalBatch.readRequest(
                    new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8))
            );

            if (entries.isEmpty() || GraphicsEnvironment.isHeadless()) {
                if (GraphicsEnvironment.isHeadless()) {
                    System.err.println("Approval window cannot start in a headless JVM; pending plugins will be skipped");
                }
                ZomboidApprovalBatch.writeResponse(
                        new BufferedWriter(new OutputStreamWriter(protocol, StandardCharsets.UTF_8)),
                        new ZomboidApprovalResponse(false, List.of())
                );
                System.exit(0);
                return;
            }

            try {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
                ZomboidApprovalTheme.apply();
            } catch (Exception ignored) {
            }

            AtomicReference<ZomboidApprovalResponse> result =
                    new AtomicReference<>(new ZomboidApprovalResponse(false, List.of()));
            SwingUtilities.invokeAndWait(() -> result.set(ApprovalDialog.show(entries)));

            ZomboidApprovalBatch.writeResponse(
                    new BufferedWriter(new OutputStreamWriter(protocol, StandardCharsets.UTF_8)),
                    result.get()
            );
            System.exit(0);
        } catch (Throwable throwable) {
            FatalErrors.rethrowIfFatal(throwable);
            throwable.printStackTrace(System.err);
            System.exit(3);
        }
    }
}
