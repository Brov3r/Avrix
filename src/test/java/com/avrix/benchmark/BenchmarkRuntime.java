package com.avrix.benchmark;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Shared counters for the realistic plugin loading benchmark.
 */
public final class BenchmarkRuntime {

    private static final AtomicInteger COMPLETED = new AtomicInteger();
    private static final AtomicLong PAYLOAD_BYTES = new AtomicLong();

    private BenchmarkRuntime() {
    }

    /**
     * Records a completed plugin initialization.
     *
     * @param pluginId plugin identifier
     * @param payload  payload read from the plugin JAR
     */
    public static void record(String pluginId, String payload) {
        if (pluginId == null || pluginId.isBlank()) {
            throw new IllegalStateException("Plugin ID cannot be blank");
        }
        COMPLETED.incrementAndGet();
        PAYLOAD_BYTES.addAndGet(payload.getBytes(java.nio.charset.StandardCharsets.UTF_8).length);
    }

    /**
     * Returns the number of completed plugin initializations.
     *
     * @return completed count
     */
    public static int completed() {
        return COMPLETED.get();
    }

    /**
     * Returns the total number of payload bytes read by plugins.
     *
     * @return payload byte count
     */
    public static long payloadBytes() {
        return PAYLOAD_BYTES.get();
    }

    /**
     * Resets benchmark counters.
     */
    public static void reset() {
        COMPLETED.set(0);
        PAYLOAD_BYTES.set(0);
    }
}
