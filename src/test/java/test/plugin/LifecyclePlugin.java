package test.plugin;

import com.avrix.plugins.Plugin;
import com.avrix.plugins.PluginData;

/**
 * Plugin stub recording lifecycle callbacks.
 */
public class LifecyclePlugin implements Plugin {

    /** {@inheritDoc} */
    @Override
    public void onInitialize(PluginData pluginData) {
        PhaseTracker.record("init:lifecycle");
    }

    /** {@inheritDoc} */
    @Override
    public void onShutdown() {
        PhaseTracker.record("shutdown:lifecycle");
    }
}
