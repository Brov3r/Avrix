package test.mixin.preflight.beta;

import org.spongepowered.asm.mixin.Mixin;

/**
 * Preflight fixture whose target belongs to the {@code alpha} test mixin package.
 */
@Mixin(targets = "test.mixin.preflight.alpha.TargetA")
public abstract class BetaMixin {
}
