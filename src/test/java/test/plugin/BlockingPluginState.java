package test.plugin;

import java.util.concurrent.CountDownLatch;

/**
 * Shared synchronization state for {@link BlockingPlugin} tests.
 */
public final class BlockingPluginState {

    /**
     * Signals that {@link BlockingPlugin#onInitialize} has been entered.
     */
    public static final CountDownLatch ENTERED = new CountDownLatch(1);

    /**
     * Releases the blocking initialize call.
     */
    public static final CountDownLatch RELEASE = new CountDownLatch(1);

    private BlockingPluginState() {
        throw new UnsupportedOperationException("State holder cannot be instantiated");
    }
}
