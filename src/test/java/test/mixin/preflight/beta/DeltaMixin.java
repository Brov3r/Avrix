package test.mixin.preflight.beta;

import org.spongepowered.asm.mixin.Mixin;

/**
 * Preflight fixture whose package contains a target registered by the alpha fixture.
 */
@Mixin(targets = "test.mixin.preflight.shared.TargetS")
public abstract class DeltaMixin {
}
