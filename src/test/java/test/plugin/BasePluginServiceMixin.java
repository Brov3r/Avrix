package test.plugin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BasePluginService.class)
public class BasePluginServiceMixin {

    @Inject(method = "calculateValue", at = @At("HEAD"), cancellable = true)
    private void onCalculateValue(String input, CallbackInfoReturnable<String> cir) {
        cir.setReturnValue("INTERCEPTED_BY_PLUGIN_B: " + input);
    }
}