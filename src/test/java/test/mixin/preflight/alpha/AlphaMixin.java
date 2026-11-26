package test.mixin.preflight.alpha;

import org.spongepowered.asm.mixin.Mixin;

/**
 * Preflight fixture whose target belongs to the {@code beta} test mixin package.
 */
@Mixin(targets = "test.mixin.preflight.beta.TargetB")
public abstract class AlphaMixin {
}
