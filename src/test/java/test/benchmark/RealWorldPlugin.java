package test.benchmark;

import com.avrix.benchmark.BenchmarkRuntime;
import com.avrix.plugins.Plugin;
import com.avrix.plugins.PluginData;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Template entrypoint for a plugin that loads resources and applies mixins to its own class and to game code.
 * <p>
 * Each generated plugin owns a unique self target class transformed by its own mixin. It also declares a second
 * mixin against one of the shared generated game targets, similar to a real mod that patches both its own classes and
 * the game.
 */
public class RealWorldPlugin implements Plugin {

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

            String selfTargetName = readRequired(pluginData, "self-target.txt").strip();
            String gameTargetName = readRequired(pluginData, "game-target.txt").strip();
            int expectedGameMixins = Integer.parseInt(
                    readRequired(pluginData, "expected-game-mixins.txt").strip()
            );

            ClassLoader classLoader = getClass().getClassLoader();

            List<String> selfTrace = new ArrayList<>();
            String selfResult = invoke(classLoader, selfTargetName, "process", selfTrace, payload);
            if (!("SELF_RESULT:" + payload).equals(selfResult)) {
                throw new IllegalStateException("Self target returned an unexpected value: " + selfResult);
            }
            if (!selfTrace.contains("SELF_BASE:" + payload) || !selfTrace.contains("SELF_MIXIN:" + payload)) {
                throw new IllegalStateException("Self mixin was not applied for plugin [" + pluginData.id() + "]");
            }
            if (selfTrace.size() != 2) {
                throw new IllegalStateException(
                        "Expected exactly one self mixin for plugin [" + pluginData.id() + "], observed "
                                + (selfTrace.size() - 1)
                );
            }

            List<String> gameTrace = new ArrayList<>();
            String gameResult = invoke(classLoader, gameTargetName, "apply", gameTrace, payload);
            if (!("RESULT:" + payload).equals(gameResult)) {
                throw new IllegalStateException("Game target returned an unexpected value: " + gameResult);
            }
            if (!gameTrace.contains("BASE:" + payload) || !gameTrace.contains("MIXIN:" + payload)) {
                throw new IllegalStateException("Game mixin was not applied for plugin [" + pluginData.id() + "]");
            }
            if (gameTrace.size() != expectedGameMixins + 1) {
                throw new IllegalStateException(
                        "Expected %d game mixins for [%s], observed %d"
                                .formatted(expectedGameMixins, gameTargetName, gameTrace.size() - 1)
                );
            }

            BenchmarkRuntime.recordRealWorld(
                    pluginData.id(),
                    (payload + config + lang).getBytes(StandardCharsets.UTF_8).length,
                    selfTrace.size(),
                    gameTrace.size()
            );
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to invoke mixin targets for plugin [" + pluginData.id() + "]", e);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String invoke(
            ClassLoader classLoader,
            String targetName,
            String methodName,
            List<String> trace,
            String payload
    ) throws ReflectiveOperationException {
        Class<?> targetClass = Class.forName(targetName, true, classLoader);
        Object target = targetClass.getDeclaredConstructor().newInstance();
        Method method = targetClass.getMethod(methodName, List.class, String.class);
        return (String) method.invoke(target, trace, payload);
    }

    private static String readRequired(PluginData pluginData, String resource) throws IOException {
        return pluginData.readResourceString(resource)
                .filter(value -> !value.isEmpty())
                .orElseThrow(() -> new IllegalStateException(
                        "Plugin [" + pluginData.id() + "] is missing required resource: " + resource
                ));
    }
}
