package com.avrix.benchmark;

import com.avrix.plugins.Plugin;
import com.avrix.plugins.PluginData;

import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * Plugin entrypoint used by the realistic loading benchmark.
 */
public class BenchmarkPlugin implements Plugin {

    /** {@inheritDoc} */
    @Override
    public void onInitialize(PluginData pluginData) {
        try {
            String payload = pluginData.readResourceString("payload.txt").orElse("");
            BenchmarkRuntime.record(pluginData.id(), payload);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
