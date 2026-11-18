package test.mixin.game;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import test.game.ExtrasGameService;

import java.util.Locale;

/**
 * MixinExtras mixin that uppercases the argument of {@link ExtrasGameService#format(String)}.
 */
@Mixin(ExtrasGameService.class)
public abstract class ExtrasGameServiceMixin {

    /**
     * Wraps the {@link String#concat(String)} call inside the target method.
     *
     * @param instance original string receiver
     * @param argument original concatenation argument
     * @param original original operation handle
     * @return concatenation result with an upper-cased argument
     */
    @WrapOperation(
            method = "format",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/lang/String;concat(Ljava/lang/String;)Ljava/lang/String;"
            )
    )
    private String wrapConcat(String instance, String argument, Operation<String> original) {
        return original.call(instance, argument.toUpperCase(Locale.ROOT));
    }
}
