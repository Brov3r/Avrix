package test.benchmark;

import com.avrix.benchmark.BenchmarkRuntime;
import com.avrix.plugins.Plugin;
import com.avrix.plugins.PluginData;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Template entrypoint for a plugin that declares a mixin and verifies the transformed target at startup.
 * <p>
 * The benchmark copies this class into each plugin JAR under a unique name. The plugin reads its target class from
 * {@code target.txt} and the expected number of registered injections from {@code expected-mixins.txt}.
 */
public class MixinPlugin implements Plugin {

    /** {@inheritDoc} */
    @Override
    public void onInitialize(PluginData pluginData) {
        try {
            String payload = readRequired(pluginData, "payload.txt");
            String targetName = readRequired(pluginData, "target.txt").strip();
            int expectedMixins = Integer.parseInt(readRequired(pluginData, "expected-mixins.txt").strip());

            Class<?> targetClass = Class.forName(targetName, true, getClass().getClassLoader());
            Object target = targetClass.getDeclaredConstructor().newInstance();
            Method apply = targetClass.getMethod("apply", List.class, String.class);

            List<String> trace = new ArrayList<>();
            String result = (String) apply.invoke(target, trace, payload);

            if (!("RESULT:" + payload).equals(result)) {
                throw new IllegalStateException("Target method returned an unexpected value: " + result);
            }
            if (!trace.contains("BASE:" + payload)) {
                throw new IllegalStateException("Target method body did not execute for plugin [" + pluginData.id() + "]");
            }
            if (!trace.contains("MIXIN:" + payload)) {
                throw new IllegalStateException("Mixin was not applied for plugin [" + pluginData.id() + "]");
            }
            if (trace.size() != expectedMixins + 1) {
                throw new IllegalStateException(
                        "Expected %d mixin injections for [%s], observed %d"
                                .formatted(expectedMixins, targetName, trace.size() - 1)
                );
            }

            BenchmarkRuntime.recordMixin(pluginData.id(), targetName, trace.size());
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to invoke mixin target for plugin [" + pluginData.id() + "]", e);
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
