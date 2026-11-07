package com.avrix.utils;

import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies propagation of direct and wrapped JVM failures without exhausting JVM resources.
 */
public class FatalErrorsTest {

    /** Verifies propagation of the original fatal error instance through wrapper exceptions. */
    @Test
    public void shouldRethrowWrappedVmFailure() {
        var fatal = new SimulatedVmFailure();
        assertThatThrownBy(() -> FatalErrors.rethrowIfFatal(fatal)).isSameAs(fatal);
        assertThatThrownBy(() -> FatalErrors.rethrowIfFatal(
                new ExecutionException(new InvocationTargetException(fatal)))).isSameAs(fatal);
    }

    /** Verifies that plugin assertions and cyclic nonfatal cause chains remain recoverable. */
    @Test
    public void shouldIgnoreNonfatalErrorsAndCauseCycles() {
        var first = new RuntimeException("first");
        var second = new RuntimeException("second", first);
        first.initCause(second);
        assertThatCode(() -> FatalErrors.rethrowIfFatal(first)).doesNotThrowAnyException();
        assertThatCode(() -> FatalErrors.rethrowIfFatal(new AssertionError("Plugin assertion")))
                .doesNotThrowAnyException();
        assertThatCode(() -> FatalErrors.rethrowIfFatal(null)).doesNotThrowAnyException();
    }

    private static final class SimulatedVmFailure extends VirtualMachineError {
    }
}
