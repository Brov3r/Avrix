package test.plugin;

import com.avrix.plugins.Plugin;
import com.avrix.plugins.PluginData;

/**
 * Records cleanup after an initialization failure.
 */
public class PartiallyInitializedPlugin implements Plugin {

    /** Creates a partially initialized plugin fixture. */
    public PartiallyInitializedPlugin() {
    }

    /** {@inheritDoc} */
    @Override
    public void onInitialize(PluginData data) {
        PhaseTracker.record("init:partial");
        throw new IllegalStateException("Partial initialization");
    }

    /** {@inheritDoc} */
    @Override
    public void onShutdown() {
        PhaseTracker.record("shutdown:partial");
    }
}
