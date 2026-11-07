package com.avrix.utils;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/**
 * Prevents fatal JVM failures from being treated as recoverable plugin errors.
 */
public final class FatalErrors {

    private FatalErrors() {
        throw new UnsupportedOperationException("FatalErrors utility class cannot be instantiated");
    }

    /**
     * Rethrows a fatal JVM failure, including one wrapped by reflection or asynchronous execution.
     *
     * @param failure failure to inspect, or {@code null}
     * @throws VirtualMachineError if the failure or one of its causes is a virtual machine failure
     * @throws ThreadDeath if the failure or one of its causes terminates the current thread
     */
    @SuppressWarnings("removal")
    public static void rethrowIfFatal(Throwable failure) {
        Set<Throwable> visited = null;
        for (Throwable current = failure; current != null; current = current.getCause()) {
            if (current instanceof VirtualMachineError fatal) {
                throw fatal;
            }
            if (current instanceof ThreadDeath fatal) {
                throw fatal;
            }
            // Check direct VM failures before allocating anything, especially when handling an OutOfMemoryError.
            if (visited == null) {
                visited = Collections.newSetFromMap(new IdentityHashMap<>());
            }
            if (!visited.add(current)) {
                return;
            }
        }
    }
}
