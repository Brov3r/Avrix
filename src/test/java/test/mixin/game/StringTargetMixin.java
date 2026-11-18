package test.mixin.game;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin that declares its target through the string form of {@code @Mixin}.
 */
@Mixin(targets = "test.game.StringTargetService")
public abstract class StringTargetMixin {

    /**
     * Replaces the target method result.
     *
     * @param callback injection callback
     */
    @Inject(method = "getValue", at = @At("HEAD"), cancellable = true)
    private void onGetValue(CallbackInfoReturnable<String> callback) {
        callback.setReturnValue("string-target");
    }
}
