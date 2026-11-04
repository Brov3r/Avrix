package test.benchmark;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * Template mixin copied into each mixin plugin JAR under a unique class name.
 */
@Mixin(MixinTargetTemplate.class)
public abstract class MixinTemplate {

    /**
     * Records a marker at the head of {@link MixinTargetTemplate#apply(List, String)}.
     *
     * @param trace execution trace
     * @param input payload to process
     * @param cir   return callback
     */
    @Inject(method = "apply", at = @At("HEAD"))
    private void onApply(List<String> trace, String input, CallbackInfoReturnable<String> cir) {
        trace.add("MIXIN:" + input);
    }
}
