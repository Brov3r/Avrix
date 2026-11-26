package test.plugin;

import com.avrix.plugins.Plugin;
import com.avrix.plugins.PluginData;

/**
 * Test plugin that emits a marker line during initialization.
 */
public final class CapturePlugin implements Plugin {

    /**
     * Marker printed from {@link #onInitialize(PluginData)}.
     */
    public static final String MARKER = "INITIALIZE_CAPTURED";

    /**
     * Creates the capture plugin for the plugin manager.
     */
    public CapturePlugin() {
    }

    /**
     * Prints the marker through the standard output stream.
     *
     * @param pluginData plugin information
     */
    @Override
    public void onInitialize(PluginData pluginData) {
        System.out.println(MARKER);
    }
}
