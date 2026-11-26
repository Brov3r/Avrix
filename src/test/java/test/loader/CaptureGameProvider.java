package test.loader;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Test provider that replaces standard output with a file so bootstrap ordering can be asserted.
 */
public final class CaptureGameProvider extends TestGameProvider {

    /**
     * Provider identifier used by the smoke test.
     */
    public static final String ID = "capture-provider";

    /**
     * System property containing the redirected output file.
     */
    public static final String REDIRECT_FILE_PROPERTY = "avrix.test.redirect.file";

    private PrintStream previousOut;

    /**
     * Creates the capture provider for {@link java.util.ServiceLoader}.
     */
    public CaptureGameProvider() {
    }

    /**
     * Returns the capture provider identifier.
     *
     * @return provider identifier
     */
    @Override
    public String getId() {
        return ID;
    }

    /**
     * Redirects standard output to the file named by {@link #REDIRECT_FILE_PROPERTY}.
     */
    @Override
    public void redirectSystemStreamsToLogger() {
        super.redirectSystemStreamsToLogger();

        String configured = System.getProperty(REDIRECT_FILE_PROPERTY);
        if (configured == null || configured.isBlank()) {
            throw new IllegalStateException("Missing system property: " + REDIRECT_FILE_PROPERTY);
        }

        try {
            previousOut = System.out;
            System.setOut(new PrintStream(Files.newOutputStream(Path.of(configured)), true, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to redirect standard output", e);
        }
    }

    /**
     * Restores standard output and releases the provider.
     */
    @Override
    public void close() {
        if (previousOut != null) {
            System.setOut(previousOut);
            previousOut = null;
        }
        super.close();
    }
}
