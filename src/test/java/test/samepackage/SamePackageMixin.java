package test.samepackage;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Invalid mixin declaration that shares its package with the target.
 */
@Mixin(SamePackageService.class)
public abstract class SamePackageMixin {

    /**
     * Injects a no-op callback into the target method.
     *
     * @param callback injection callback
     */
    @Inject(method = "getValue", at = @At("HEAD"))
    private void onGetValue(CallbackInfoReturnable<String> callback) {
    }
}
