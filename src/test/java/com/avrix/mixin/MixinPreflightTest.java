package com.avrix.mixin;

import com.avrix.core.KnotClassLoader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import net.lenni0451.classtransform.utils.FailStrategy;
import test.game.TargetGameServiceMixin;

import java.net.URL;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies that mixin preflight never changes the active transformer manager.
 */
public class MixinPreflightTest {

    /**
     * Verifies valid and invalid batches against the real registration backend.
     *
     * @throws Exception if the classloader cannot be closed
     */
    @Test
    public void shouldValidateWithoutRegistering() throws Exception {
        try (var loader = new KnotClassLoader(new URL[0], getClass().getClassLoader())) {
            var transformer = new MixinTransformer(loader);
            String valid = TargetGameServiceMixin.class.getName();
            transformer.validateMixins(List.of(valid));
            assertThat(transformer.getManager().getTransformedClasses()).isEmpty();
            assertThatThrownBy(() -> transformer.validateMixins(List.of(valid, "missing.Mixin")))
                    .isInstanceOf(RuntimeException.class);
            assertThat(transformer.getManager().getTransformedClasses()).isEmpty();
        }
    }

    /**
     * Verifies that the backend cannot swallow fatal errors and that its configured policy is restored.
     *
     * @param strict whether nonfatal transformation errors must propagate
     * @throws Exception if the classloader cannot be closed
     */
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void shouldPropagateFatalBackendFailure(boolean strict) throws Exception {
        try (var loader = new KnotClassLoader(new URL[0], getClass().getClassLoader())) {
            var transformer = new MixinTransformer(loader, strict);
            var failure = new InternalError("Simulated backend failure");
            transformer.getManager().addBytecodeTransformer((name, bytes, frames) -> { throw failure; });

            assertThatThrownBy(() -> transformer.transform("example.Target", new byte[0])).isSameAs(failure);
            assertThat(transformer.getManager().getFailStrategy()).isEqualTo(strict ? FailStrategy.THROW : FailStrategy.CANCEL);
        }
    }

    /**
     * Verifies that lenient mode still falls back to original bytecode for ordinary transformation errors.
     *
     * @throws Exception if the classloader cannot be closed
     */
    @Test
    public void shouldStillIsolateNonfatalBackendFailure() throws Exception {
        try (var loader = new KnotClassLoader(new URL[0], getClass().getClassLoader())) {
            var transformer = new MixinTransformer(loader);
            transformer.getManager().addBytecodeTransformer((name, bytes, frames) -> {
                throw new IllegalStateException("Ordinary transformation failure");
            });
            assertThat(transformer.transform("example.Target", new byte[0])).isNull();
            assertThat(transformer.getManager().getFailStrategy()).isEqualTo(FailStrategy.CANCEL);
        }
    }
}
