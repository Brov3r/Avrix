package test.benchmark;

import com.avrix.benchmark.BenchmarkRuntime;
import com.avrix.plugins.Plugin;
import com.avrix.plugins.PluginData;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/**
 * Template entrypoint for a realistic plugin that loads several resources.
 * <p>
 * The benchmark copies this class into each plugin JAR under a unique name, so every plugin contributes its own class
 * loading and initialization work instead of sharing a single parent-loaded entrypoint.
 */
public class ResourcePlugin implements Plugin {

    /** {@inheritDoc} */
    @Override
    public void onInitialize(PluginData pluginData) {
        try {
            String payload = readRequired(pluginData, "payload.txt");
            String config = readRequired(pluginData, "config.yml");
            String lang = readRequired(pluginData, "assets/lang/en_US.json");

            if (pluginData.listResources("assets", true).isEmpty()) {
                throw new IllegalStateException("Plugin [" + pluginData.id() + "] cannot list its assets");
            }

            BenchmarkRuntime.recordResource(
                    pluginData.id(),
                    (payload + config + lang).getBytes(StandardCharsets.UTF_8).length
            );
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String readRequired(PluginData pluginData, String resource) throws IOException {
        return pluginData.readResourceString(resource)
                .filter(value -> !value.isEmpty())
                .orElseThrow(() -> new IllegalStateException(
                        "Plugin [" + pluginData.id() + "] is missing required resource: " + resource
                ));
    }
}
