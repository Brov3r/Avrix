package test.game;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * First independent mixin injecting at HEAD into {@link TargetGameService#executePipeline(List, String)}.
 */
@Mixin(TargetGameService.class)
public abstract class FirstPipelineInjectMixin {

    @Inject(
            method = "executePipeline(Ljava/util/List;Ljava/lang/String;)Ljava/lang/String;",
            at = @At("HEAD")
    )
    private void injectFirstStage(List<String> traceLog, String payload, CallbackInfoReturnable<String> cir) {
        traceLog.add("INJECT_FIRST: " + payload);
    }
}