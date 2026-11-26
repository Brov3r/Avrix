import org.spongepowered.asm.mixin.Mixin;

/**
 * Preflight fixture declared in the unnamed package.
 */
@Mixin(targets = "test.mixin.preflight.shared.TargetS")
public abstract class DefaultPackageMixin {
}
