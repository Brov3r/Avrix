package test.mixin.game;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import test.game.PipelineGameService;

import java.util.List;

/**
 * Second independent mixin injecting at TAIL into {@link PipelineGameService#executePipeline(List, String)}.
 */
@Mixin(PipelineGameService.class)
public abstract class SecondPipelineInjectMixin {

    @Inject(
            method = "executePipeline(Ljava/util/List;Ljava/lang/String;)Ljava/lang/String;",
            at = @At("TAIL")
    )
    private void injectSecondStage(List<String> traceLog, String payload, CallbackInfoReturnable<String> cir) {
        traceLog.add("INJECT_SECOND: " + payload);
    }
}
