package test.mixin.game;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import test.game.PlainGameService;

/**
 * Mixin used to inspect a strict generated configuration.
 */
@Mixin(PlainGameService.class)
public abstract class StrictConfigMixin {

    /**
     * Injects a no-op callback into the target method.
     *
     * @param input target method argument
     * @param callback injection callback
     */
    @Inject(method = "getGreeting", at = @At("HEAD"))
    private void onGreeting(String input, CallbackInfoReturnable<String> callback) {
    }
}
