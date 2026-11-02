package test.plugin;

import com.avrix.plugins.Plugin;
import com.avrix.plugins.PluginData;

/**
 * Entrypoint that records when plugin initialization starts.
 */
public class PhaseAEntrypoint implements Plugin {

    /** {@inheritDoc} */
    @Override
    public void onInitialize(PluginData pluginData) {
        PhaseTracker.record("init:plugin-a");
    }
}
