package test.plugin;

import com.avrix.plugins.Plugin;
import com.avrix.plugins.PluginData;

/**
 * Isolated entrypoint for Plugin A residing outside of com.avrix.* packages.
 */
public class PluginAEntrypoint implements Plugin {

    public static String executionResult;

    @Override
    public void onInitialize(PluginData pluginData) {
        BasePluginService service = new BasePluginService();
        executionResult = service.calculateValue("InputData");
    }
}