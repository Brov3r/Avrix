package test.mixin.preflight.beta2;

import org.spongepowered.asm.mixin.Mixin;

/**
 * Preflight fixture whose package only shares a string prefix with the beta fixture package.
 */
@Mixin(targets = "test.mixin.preflight.shared.TargetS")
public abstract class PrefixSafeMixin {
}
