package com.avrix.benchmark;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Shared counters for the realistic plugin loading benchmarks.
 * <p>
 * Plugin entrypoints run inside the benchmark {@code KnotClassLoader} and report their work here. The benchmark
 * validates these counters after every iteration to detect silently skipped initialization work.
 */
public final class BenchmarkRuntime {

    private static final AtomicInteger RESOURCE_PLUGINS = new AtomicInteger();
    private static final AtomicLong RESOURCE_BYTES = new AtomicLong();
    private static final AtomicInteger MIXIN_PLUGINS = new AtomicInteger();
    private static final AtomicLong MIXIN_TRACE_ENTRIES = new AtomicLong();
    private static final AtomicInteger REAL_WORLD_PLUGINS = new AtomicInteger();
    private static final AtomicLong REAL_WORLD_RESOURCE_BYTES = new AtomicLong();
    private static final AtomicLong SELF_MIXIN_APPLICATIONS = new AtomicLong();
    private static final AtomicLong GAME_MIXIN_APPLICATIONS = new AtomicLong();

    private BenchmarkRuntime() {
    }

    /**
     * Records a completed resource-loading plugin initialization.
     *
     * @param pluginId plugin identifier
     * @param bytes    number of resource bytes consumed by the plugin
     */
    public static void recordResource(String pluginId, long bytes) {
        if (pluginId == null || pluginId.isBlank()) {
            throw new IllegalStateException("Plugin ID cannot be blank");
        }
        if (bytes <= 0) {
            throw new IllegalStateException("Plugin [" + pluginId + "] did not read any resources");
        }
        RESOURCE_PLUGINS.incrementAndGet();
        RESOURCE_BYTES.addAndGet(bytes);
    }

    /**
     * Records a completed mixin plugin initialization.
     *
     * @param pluginId    plugin identifier
     * @param targetClass transformed target class name
     * @param traceSize   number of recorded target method invocations
     */
    public static void recordMixin(String pluginId, String targetClass, int traceSize) {
        if (pluginId == null || pluginId.isBlank()) {
            throw new IllegalStateException("Plugin ID cannot be blank");
        }
        if (targetClass == null || targetClass.isBlank()) {
            throw new IllegalStateException("Target class cannot be blank");
        }
        if (traceSize <= 0) {
            throw new IllegalStateException("Mixin plugin [" + pluginId + "] did not observe a transformed target");
        }
        MIXIN_PLUGINS.incrementAndGet();
        MIXIN_TRACE_ENTRIES.addAndGet(traceSize);
    }

    /**
     * Records a completed real-world plugin initialization that touched resources and both mixin targets.
     *
     * @param pluginId       plugin identifier
     * @param resourceBytes  number of resource bytes consumed by the plugin
     * @param selfTraceSize  number of trace entries produced by the plugin's own transformed class
     * @param gameTraceSize  number of trace entries produced by the transformed game target
     */
    public static void recordRealWorld(String pluginId, long resourceBytes, int selfTraceSize, int gameTraceSize) {
        if (pluginId == null || pluginId.isBlank()) {
            throw new IllegalStateException("Plugin ID cannot be blank");
        }
        if (resourceBytes <= 0) {
            throw new IllegalStateException("Plugin [" + pluginId + "] did not read any resources");
        }
        if (selfTraceSize < 2) {
            throw new IllegalStateException("Plugin [" + pluginId + "] did not observe its self mixin");
        }
        if (gameTraceSize < 2) {
            throw new IllegalStateException("Plugin [" + pluginId + "] did not observe its game mixin");
        }
        REAL_WORLD_PLUGINS.incrementAndGet();
        REAL_WORLD_RESOURCE_BYTES.addAndGet(resourceBytes);
        SELF_MIXIN_APPLICATIONS.addAndGet(selfTraceSize - 1);
        GAME_MIXIN_APPLICATIONS.addAndGet(gameTraceSize - 1);
    }

    /**
     * Returns the number of completed resource-loading plugin initializations.
     *
     * @return completed count
     */
    public static int resourcePlugins() {
        return RESOURCE_PLUGINS.get();
    }

    /**
     * Returns the total number of resource bytes read by plugins.
     *
     * @return resource byte count
     */
    public static long resourceBytes() {
        return RESOURCE_BYTES.get();
    }

    /**
     * Returns the number of completed mixin plugin initializations.
     *
     * @return completed count
     */
    public static int mixinPlugins() {
        return MIXIN_PLUGINS.get();
    }

    /**
     * Returns the total number of target method trace entries observed by mixin plugins.
     *
     * @return trace entry count
     */
    public static long mixinTraceEntries() {
        return MIXIN_TRACE_ENTRIES.get();
    }

    /**
     * Returns the number of completed real-world plugin initializations.
     *
     * @return completed count
     */
    public static int realWorldPlugins() {
        return REAL_WORLD_PLUGINS.get();
    }

    /**
     * Returns the total number of resource bytes read by real-world plugins.
     *
     * @return resource byte count
     */
    public static long realWorldResourceBytes() {
        return REAL_WORLD_RESOURCE_BYTES.get();
    }

    /**
     * Returns the number of self mixin applications observed by real-world plugins.
     *
     * @return self mixin application count
     */
    public static long selfMixinApplications() {
        return SELF_MIXIN_APPLICATIONS.get();
    }

    /**
     * Returns the number of game mixin applications observed by real-world plugins.
     *
     * @return game mixin application count
     */
    public static long gameMixinApplications() {
        return GAME_MIXIN_APPLICATIONS.get();
    }

    /**
     * Resets benchmark counters.
     */
    public static void reset() {
        RESOURCE_PLUGINS.set(0);
        RESOURCE_BYTES.set(0);
        MIXIN_PLUGINS.set(0);
        MIXIN_TRACE_ENTRIES.set(0);
        REAL_WORLD_PLUGINS.set(0);
        REAL_WORLD_RESOURCE_BYTES.set(0);
        SELF_MIXIN_APPLICATIONS.set(0);
        GAME_MIXIN_APPLICATIONS.set(0);
    }
}
