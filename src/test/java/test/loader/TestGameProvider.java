package test.loader;

import com.avrix.core.Environment;
import com.avrix.api.GameProvider;

import java.nio.file.Path;
import java.util.List;

/**
 * Test-only game provider used to exercise ServiceLoader discovery and loader runtime wiring.
 */
public class TestGameProvider implements GameProvider {

    /**
     * Provider identifier registered in the test service descriptor.
     */
    public static final String ID = "test-provider";

    /**
     * Number of {@link #init()} invocations.
     */
    public static volatile int initCalls;

    /**
     * Number of {@link #launch(String[])} invocations.
     */
    public static volatile int launchCalls;

    /**
     * Number of {@link #redirectSystemStreamsToLogger()} invocations.
     */
    public static volatile int redirectCalls;

    /**
     * Number of {@link #close()} invocations.
     */
    public static volatile int closeCalls;

    /**
     * Whether {@link #init()} should fail with a simulated error.
     */
    public static volatile boolean failInit;

    /**
     * Environment reported by this provider.
     */
    public static volatile Environment environment = Environment.BOTH;

    /** Game root used when resolving relative loader paths. */
    public static volatile Path launchDirectory = Path.of(".");

    /**
     * Arguments captured by the most recent {@link #launch(String[])} call.
     */
    public static volatile String[] lastLaunchArgs = new String[0];

    /**
     * Resets all recorded invocation state.
     */
    public static void reset() {
        initCalls = 0;
        launchCalls = 0;
        redirectCalls = 0;
        closeCalls = 0;
        failInit = false;
        environment = Environment.BOTH;
        launchDirectory = Path.of(".");
        lastLaunchArgs = new String[0];
    }

    /** {@inheritDoc} */
    @Override
    public void init() {
        if (failInit) {
            throw new IllegalStateException("simulated provider init failure");
        }
        initCalls++;
    }

    /** {@inheritDoc} */
    @Override
    public void launch(String[] args) {
        launchCalls++;
        lastLaunchArgs = (args == null) ? new String[0] : args.clone();
    }

    /** {@inheritDoc} */
    @Override
    public String[] getLaunchArgs() {
        return lastLaunchArgs.clone();
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return "Test Game";
    }

    /** {@inheritDoc} */
    @Override
    public String getId() {
        return ID;
    }

    /** {@inheritDoc} */
    @Override
    public List<String> getAuthors() {
        return List.of("Test Author");
    }

    /** {@inheritDoc} */
    @Override
    public String getLicense() {
        return "MIT";
    }

    /** {@inheritDoc} */
    @Override
    public List<String> getContacts() {
        return List.of("https://example.test/avrix");
    }

    /** {@inheritDoc} */
    @Override
    public String getNormalizedVersion() {
        return "1.0.0";
    }

    /** {@inheritDoc} */
    @Override
    public String getRawVersion() {
        return "1.0.0";
    }

    /** {@inheritDoc} */
    @Override
    public Path getLaunchDirectory() {
        return launchDirectory;
    }

    /** {@inheritDoc} */
    @Override
    public String getEntrypoint() {
        return "test.loader.TestGame";
    }

    /** {@inheritDoc} */
    @Override
    public Environment getEnvironment() {
        return environment;
    }

    /** {@inheritDoc} */
    @Override
    public void redirectSystemStreamsToLogger() {
        redirectCalls++;
    }

    @Override
    public void close() {
        closeCalls++;
    }

    /**
     * Secondary provider sharing {@link #ID} to exercise duplicate-ID detection.
     */
    public static final class Duplicate extends TestGameProvider {
    }
}
