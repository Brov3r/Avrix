package test.plugin;

import com.avrix.plugins.Plugin;
import com.avrix.plugins.PluginData;

/**
 * Plugin stub that records its shutdown callback and then fails intentionally.
 */
public class ShutdownFailingPlugin implements Plugin {

    /** {@inheritDoc} */
    @Override
    public void onInitialize(PluginData pluginData) {
        PhaseTracker.record("init:shutdown-failing");
    }

    /** {@inheritDoc} */
    @Override
    public void onShutdown() {
        PhaseTracker.record("shutdown:shutdown-failing");
        throw new IllegalStateException("Intentional shutdown failure");
    }
}
