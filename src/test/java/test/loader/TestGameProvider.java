package test.loader;

import com.avrix.core.Environment;
import com.avrix.provider.GameProvider;

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
        lastLaunchArgs = new String[0];
    }

    /** {@inheritDoc} */
    @Override
    public void init() {
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
        return Path.of(".");
    }

    /** {@inheritDoc} */
    @Override
    public String getEntrypoint() {
        return "test.loader.TestGame";
    }

    /** {@inheritDoc} */
    @Override
    public Environment getEnvironment() {
        return Environment.BOTH;
    }

    /** {@inheritDoc} */
    @Override
    public void redirectSystemStreamsToLogger() {
        redirectCalls++;
    }

    /**
     * Secondary provider sharing {@link #ID} to exercise duplicate-ID detection.
     */
    public static final class Duplicate extends TestGameProvider {
    }
}
