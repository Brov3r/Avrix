package test.plugin;

import com.avrix.plugins.Plugin;
import com.avrix.plugins.PluginData;

import java.util.concurrent.TimeUnit;

/**
 * Plugin entrypoint that blocks inside {@link #onInitialize(PluginData)} until released.
 */
public final class BlockingPlugin implements Plugin {

    /** {@inheritDoc} */
    @Override
    public void onInitialize(PluginData pluginData) {
        BlockingPluginState.ENTERED.countDown();
        try {
            BlockingPluginState.RELEASE.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
