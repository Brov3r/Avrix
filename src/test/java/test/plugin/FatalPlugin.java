package test.plugin;

import com.avrix.plugins.Plugin;
import com.avrix.plugins.PluginData;

/**
 * Produces a simulated fatal JVM failure from a plugin entrypoint.
 */
public class FatalPlugin implements Plugin {

    /** Creates the fatal plugin fixture. */
    public FatalPlugin() {
    }

    /** {@inheritDoc} */
    @Override
    public void onInitialize(PluginData data) {
        throw new InternalError("Simulated fatal plugin failure");
    }
}
