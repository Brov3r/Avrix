package test.plugin;

import com.avrix.plugins.Plugin;
import com.avrix.plugins.PluginData;

/**
 * Plugin stub that fails during entrypoint initialization.
 */
public class ErrorThrowingPlugin implements Plugin {

    /** {@inheritDoc} */
    @Override
    public void onInitialize(PluginData pluginData) {
        throw new AssertionError("Intentional test failure");
    }
}
