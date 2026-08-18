package test.game;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

/**
 * SpongePowered Mixin targeting {@link TargetGameService}.
 */
@Mixin(TargetGameService.class)
public abstract class TargetGameServiceMixin {

    /**
     * Overwrites original {@link TargetGameService#getGreeting(String)}.
     *
     * @param input input name
     * @return intercepted and modified greeting
     * @author AvrixTest
     * @reason Integration test verification
     */
    @Overwrite
    public String getGreeting(String input) {
        return "Intercepted by Avrix Mixin: " + input;
    }
}