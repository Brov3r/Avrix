package test.mixin.game;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import test.game.PipelineGameService;

import java.util.List;

/**
 * First independent mixin injecting at HEAD into {@link PipelineGameService#executePipeline(List, String)}.
 */
@Mixin(PipelineGameService.class)
public abstract class FirstPipelineInjectMixin {

    @Inject(
            method = "executePipeline(Ljava/util/List;Ljava/lang/String;)Ljava/lang/String;",
            at = @At("HEAD")
    )
    private void injectFirstStage(List<String> traceLog, String payload, CallbackInfoReturnable<String> cir) {
        traceLog.add("INJECT_FIRST: " + payload);
    }
}
