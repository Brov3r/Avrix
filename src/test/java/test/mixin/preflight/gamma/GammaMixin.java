package test.mixin.preflight.gamma;

import org.spongepowered.asm.mixin.Mixin;

/**
 * Preflight fixture whose target belongs to the registered {@code alpha} test mixin package.
 */
@Mixin(targets = "test.mixin.preflight.alpha.TargetA")
public abstract class GammaMixin {
}
