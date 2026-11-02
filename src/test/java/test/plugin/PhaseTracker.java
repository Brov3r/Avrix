package test.plugin;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Shared event recorder for two-phase plugin loading tests.
 */
public final class PhaseTracker {

    private static final List<String> EVENTS = new CopyOnWriteArrayList<>();

    private PhaseTracker() {
    }

    /**
     * Records an event.
     *
     * @param event event description
     */
    public static void record(String event) {
        EVENTS.add(event);
    }

    /**
     * Returns recorded events in insertion order.
     *
     * @return immutable event snapshot
     */
    public static List<String> events() {
        return List.copyOf(EVENTS);
    }

    /**
     * Clears recorded events.
     */
    public static void reset() {
        EVENTS.clear();
    }
}
