package test.benchmark;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * Template mixin that modifies a class owned by the same plugin.
 */
@Mixin(SelfTargetTemplate.class)
public abstract class SelfMixinTemplate {

    /**
     * Records a marker at the head of {@link SelfTargetTemplate#process(List, String)}.
     *
     * @param trace execution trace
     * @param input payload to process
     * @param cir   return callback
     */
    @Inject(method = "process", at = @At("HEAD"))
    private void onProcess(List<String> trace, String input, CallbackInfoReturnable<String> cir) {
        trace.add("SELF_MIXIN:" + input);
    }
}
