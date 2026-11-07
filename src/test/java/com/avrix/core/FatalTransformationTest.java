package com.avrix.core;

import org.junit.jupiter.api.Test;

import java.net.URL;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies that lenient bytecode transformation does not suppress VM failures.
 */
public class FatalTransformationTest {

    /**
     * Verifies that fatal transformation failures escape unchanged in lenient mode.
     *
     * @throws Exception if the classloader cannot be closed
     */
    @Test
    public void shouldRethrowVmFailureInLenientMode() throws Exception {
        var failure = new SimulatedVmFailure();
        try (var loader = new KnotClassLoader(new URL[0], getClass().getClassLoader())) {
            loader.setClassTransformer((name, bytes) -> { throw failure; });
            assertThatThrownBy(() -> loader.transformClass("example.Target", new byte[0])).isSameAs(failure);
        }
    }

    private static final class SimulatedVmFailure extends VirtualMachineError {
    }
}
