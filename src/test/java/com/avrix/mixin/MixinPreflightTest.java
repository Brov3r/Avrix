package com.avrix.mixin;

import com.avrix.core.KnotClassLoader;
import org.junit.jupiter.api.Test;
import org.spongepowered.asm.mixin.Mixins;
import org.spongepowered.asm.mixin.transformer.Config;
import test.game.TargetGameService;
import test.mixin.game.LenientConfigMixin;
import test.mixin.game.NoTargetMixin;
import test.mixin.game.StrictConfigMixin;
import test.mixin.game.TargetGameServiceMixin;
import test.samepackage.SamePackageMixin;

import java.net.URL;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies that mixin preflight validates declarations without registering them.
 */
public class MixinPreflightTest {

    /**
     * Verifies valid and invalid batches against the real registration backend.
     *
     * @throws Exception if the classloader cannot be closed
     */
    @Test
    public void shouldValidateWithoutRegistering() throws Exception {
        try (var loader = new KnotClassLoader(new URL[0], getClass().getClassLoader())) {
            var transformer = new MixinTransformer(loader);
            String valid = TargetGameServiceMixin.class.getName();
            int configsBefore = Mixins.getConfigs().size();

            transformer.validateMixins(List.of(valid));

            assertThat(transformer.isMixinRegistered(valid)).isFalse();
            assertThat(Mixins.getConfigs()).hasSize(configsBefore);

            assertThatThrownBy(() -> transformer.validateMixins(List.of(valid, "missing.Mixin")))
                    .isInstanceOf(RuntimeException.class);

            assertThat(transformer.isMixinRegistered(valid)).isFalse();
            assertThat(Mixins.getConfigs()).hasSize(configsBefore);
        }
    }

    /**
     * Verifies that non-mixin classes and invalid declarations are rejected before registration.
     *
     * @throws Exception if the classloader cannot be closed
     */
    @Test
    public void shouldRejectInvalidDeclarations() throws Exception {
        try (var loader = new KnotClassLoader(new URL[0], getClass().getClassLoader())) {
            var transformer = new MixinTransformer(loader);

            assertThatThrownBy(() -> transformer.validateMixins(List.of(TargetGameService.class.getName())))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("not annotated with @Mixin");
            assertThatThrownBy(() -> transformer.validateMixins(List.of(NoTargetMixin.class.getName())))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("does not declare any target");
            assertThatThrownBy(() -> transformer.validateMixins(List.of(" ")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("cannot be blank");
            assertThatThrownBy(() -> transformer.validateMixins(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("Mixins collection");
        }
    }

    /**
     * Verifies that a mixin cannot share its package with its target.
     *
     * @throws Exception if the classloader cannot be closed
     */
    @Test
    public void shouldRejectMixinSharingTargetPackage() throws Exception {
        try (var loader = new KnotClassLoader(new URL[0], getClass().getClassLoader())) {
            var transformer = new MixinTransformer(loader);

            assertThatThrownBy(() -> transformer.addMixin(SamePackageMixin.class.getName()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must not share its package");
            assertThat(transformer.isMixinRegistered(SamePackageMixin.class.getName())).isFalse();
        }
    }

    /**
     * Verifies that the strict flag is propagated into the generated configuration.
     *
     * @throws Exception if the classloader cannot be closed
     */
    @Test
    public void shouldConfigureRequiredFlagFromStrictMode() throws Exception {
        try (var loader = new KnotClassLoader(new URL[0], getClass().getClassLoader())) {
            var lenient = new MixinTransformer(loader, false);
            Set<Config> beforeLenient = Set.copyOf(Mixins.getConfigs());
            lenient.addMixin(LenientConfigMixin.class.getName());
            assertThat(newConfig(beforeLenient).getConfig().isRequired()).isFalse();

            var strict = new MixinTransformer(loader, true);
            Set<Config> beforeStrict = Set.copyOf(Mixins.getConfigs());
            strict.addMixin(StrictConfigMixin.class.getName());
            assertThat(newConfig(beforeStrict).getConfig().isRequired()).isTrue();
        }
    }

    private static Config newConfig(Set<Config> before) {
        return Mixins.getConfigs().stream()
                .filter(config -> !before.contains(config))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No mixin configuration was registered"));
    }
}
