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
import test.mixin.preflight.alpha.AlphaMixin;
import test.mixin.preflight.beta.BetaMixin;
import test.mixin.preflight.beta.DeltaMixin;
import test.mixin.preflight.beta2.PrefixSafeMixin;
import test.mixin.preflight.gamma.GammaMixin;
import test.samepackage.SamePackageMixin;

import java.net.URL;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies that mixin preflight validates declarations and package conflicts without registering them.
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
     * Verifies that a mixin cannot share its package with its target during preflight or direct registration.
     *
     * @throws Exception if the classloader cannot be closed
     */
    @Test
    public void shouldRejectMixinSharingTargetPackage() throws Exception {
        try (var loader = new KnotClassLoader(new URL[0], getClass().getClassLoader())) {
            var transformer = new MixinTransformer(loader);
            String mixin = SamePackageMixin.class.getName();
            int configsBefore = Mixins.getConfigs().size();

            assertThatThrownBy(() -> transformer.validateMixins(List.of(mixin)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must not share its package");
            assertThat(transformer.isMixinRegistered(mixin)).isFalse();
            assertThat(Mixins.getConfigs()).hasSize(configsBefore);

            assertThatThrownBy(() -> transformer.addMixin(mixin))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must not share its package");
            assertThat(transformer.isMixinRegistered(mixin)).isFalse();
            assertThat(Mixins.getConfigs()).hasSize(configsBefore);
        }
    }

    /**
     * Verifies that a mixin declared in the unnamed package is rejected during preflight and direct registration.
     *
     * @throws Exception if the classloader cannot be closed
     */
    @Test
    public void shouldRejectMixinWithoutNamedPackage() throws Exception {
        try (var loader = new KnotClassLoader(new URL[0], getClass().getClassLoader())) {
            var transformer = new MixinTransformer(loader);
            String mixin = "DefaultPackageMixin";
            int configsBefore = Mixins.getConfigs().size();

            assertThatThrownBy(() -> transformer.validateMixins(List.of(mixin)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must be declared in a named package");
            assertThat(transformer.isMixinRegistered(mixin)).isFalse();
            assertThat(Mixins.getConfigs()).hasSize(configsBefore);

            assertThatThrownBy(() -> transformer.addMixin(mixin))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must be declared in a named package");
            assertThat(transformer.isMixinRegistered(mixin)).isFalse();
            assertThat(Mixins.getConfigs()).hasSize(configsBefore);
        }
    }

    /**
     * Verifies that targets are checked against every mixin package declared in the same batch regardless of order.
     *
     * @throws Exception if the classloader cannot be closed
     */
    @Test
    public void shouldRejectBatchPackageConflictsRegardlessOfOrder() throws Exception {
        try (var loader = new KnotClassLoader(new URL[0], getClass().getClassLoader())) {
            var transformer = new MixinTransformer(loader);
            String alpha = AlphaMixin.class.getName();
            String beta = BetaMixin.class.getName();
            int configsBefore = Mixins.getConfigs().size();

            assertThatThrownBy(() -> transformer.validateMixins(List.of(alpha, beta)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("inside mixin package");
            assertThat(transformer.isMixinRegistered(alpha)).isFalse();
            assertThat(transformer.isMixinRegistered(beta)).isFalse();
            assertThat(Mixins.getConfigs()).hasSize(configsBefore);

            assertThatThrownBy(() -> transformer.validateMixins(List.of(beta, alpha)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("inside mixin package");
            assertThat(transformer.isMixinRegistered(alpha)).isFalse();
            assertThat(transformer.isMixinRegistered(beta)).isFalse();
            assertThat(Mixins.getConfigs()).hasSize(configsBefore);
        }
    }

    /**
     * Verifies that a new target cannot belong to the package of an already registered mixin.
     *
     * @throws Exception if the classloader cannot be closed
     */
    @Test
    public void shouldRejectTargetInsideRegisteredMixinPackage() throws Exception {
        try (var loader = new KnotClassLoader(new URL[0], getClass().getClassLoader())) {
            var transformer = new MixinTransformer(loader);
            String alpha = AlphaMixin.class.getName();
            String gamma = GammaMixin.class.getName();

            transformer.addMixin(alpha);
            int configsAfterRegistration = Mixins.getConfigs().size();

            assertThatThrownBy(() -> transformer.validateMixins(List.of(gamma)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("inside mixin package");
            assertThatThrownBy(() -> transformer.validateMixins(List.of(alpha, gamma)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("inside mixin package");
            assertThat(transformer.isMixinRegistered(alpha)).isTrue();
            assertThat(transformer.isMixinRegistered(gamma)).isFalse();
            assertThat(Mixins.getConfigs()).hasSize(configsAfterRegistration);
        }
    }

    /**
     * Verifies that a new mixin package cannot contain a target of an already registered mixin.
     *
     * @throws Exception if the classloader cannot be closed
     */
    @Test
    public void shouldRejectRegisteredTargetInsideNewMixinPackage() throws Exception {
        try (var loader = new KnotClassLoader(new URL[0], getClass().getClassLoader())) {
            var transformer = new MixinTransformer(loader);
            String alpha = AlphaMixin.class.getName();
            String delta = DeltaMixin.class.getName();

            transformer.addMixin(alpha);
            int configsAfterRegistration = Mixins.getConfigs().size();

            assertThatThrownBy(() -> transformer.validateMixins(List.of(delta)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("inside mixin package");
            assertThatThrownBy(() -> transformer.validateMixins(List.of(alpha, delta)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("inside mixin package");
            assertThat(transformer.isMixinRegistered(alpha)).isTrue();
            assertThat(transformer.isMixinRegistered(delta)).isFalse();
            assertThat(Mixins.getConfigs()).hasSize(configsAfterRegistration);
        }
    }

    /**
     * Verifies that a package sharing only a string prefix with a registered mixin package is not rejected.
     *
     * @throws Exception if the classloader cannot be closed
     */
    @Test
    public void shouldNotTreatSimilarlyPrefixedPackageAsConflict() throws Exception {
        try (var loader = new KnotClassLoader(new URL[0], getClass().getClassLoader())) {
            var transformer = new MixinTransformer(loader);
            String alpha = AlphaMixin.class.getName();
            String safe = PrefixSafeMixin.class.getName();

            transformer.addMixin(alpha);
            transformer.validateMixins(List.of(safe));
            transformer.addMixin(safe);

            assertThat(transformer.isMixinRegistered(alpha)).isTrue();
            assertThat(transformer.isMixinRegistered(safe)).isTrue();
        }
    }

    /**
     * Verifies that a valid batch is registered after preflight.
     *
     * @throws Exception if the classloader cannot be closed
     */
    @Test
    public void shouldRegisterValidBatchAfterPreflight() throws Exception {
        try (var loader = new KnotClassLoader(new URL[0], getClass().getClassLoader())) {
            var transformer = new MixinTransformer(loader);
            String alpha = AlphaMixin.class.getName();
            String safe = PrefixSafeMixin.class.getName();

            transformer.addMixins(List.of(alpha, safe));

            assertThat(transformer.isMixinRegistered(alpha)).isTrue();
            assertThat(transformer.isMixinRegistered(safe)).isTrue();
        }
    }

    /**
     * Verifies that direct registration rejects a package conflict without leaving partial state behind.
     *
     * @throws Exception if the classloader cannot be closed
     */
    @Test
    public void shouldRejectDirectRegistrationConflictWithoutPartialState() throws Exception {
        try (var loader = new KnotClassLoader(new URL[0], getClass().getClassLoader())) {
            var transformer = new MixinTransformer(loader);
            String alpha = AlphaMixin.class.getName();
            String beta = BetaMixin.class.getName();

            transformer.addMixin(alpha);
            int configsAfterRegistration = Mixins.getConfigs().size();

            assertThatThrownBy(() -> transformer.addMixin(beta))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("inside mixin package");
            assertThat(transformer.isMixinRegistered(alpha)).isTrue();
            assertThat(transformer.isMixinRegistered(beta)).isFalse();
            assertThat(Mixins.getConfigs()).hasSize(configsAfterRegistration);
        }
    }

    /**
     * Verifies that a batch rejected by preflight does not register any of its mixins.
     *
     * @throws Exception if the classloader cannot be closed
     */
    @Test
    public void shouldNotPartiallyRegisterRejectedBatch() throws Exception {
        try (var loader = new KnotClassLoader(new URL[0], getClass().getClassLoader())) {
            var transformer = new MixinTransformer(loader);
            String alpha = AlphaMixin.class.getName();
            String beta = BetaMixin.class.getName();
            int configsBefore = Mixins.getConfigs().size();

            assertThatThrownBy(() -> transformer.addMixins(List.of(alpha, beta)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("inside mixin package");
            assertThat(transformer.isMixinRegistered(alpha)).isFalse();
            assertThat(transformer.isMixinRegistered(beta)).isFalse();
            assertThat(Mixins.getConfigs()).hasSize(configsBefore);

            assertThatThrownBy(() -> transformer.addMixins(List.of(alpha, "missing.Mixin")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Mixin class not found");
            assertThat(transformer.isMixinRegistered(alpha)).isFalse();
            assertThat(Mixins.getConfigs()).hasSize(configsBefore);
        }
    }

    /**
     * Verifies that registered mixins stay idempotent and target classes are not loaded during validation or
     * registration.
     *
     * @throws Exception if the classloader cannot be closed
     */
    @Test
    public void shouldNotLoadTargetsAndKeepRegistrationIdempotent() throws Exception {
        try (var loader = new KnotClassLoader(new URL[0], getClass().getClassLoader())) {
            var transformer = new MixinTransformer(loader);
            String alpha = AlphaMixin.class.getName();

            transformer.validateMixins(List.of(alpha));
            transformer.addMixin(alpha);
            int configsAfterRegistration = Mixins.getConfigs().size();

            transformer.validateMixins(List.of(alpha, alpha));
            transformer.addMixin(alpha);

            assertThat(transformer.isMixinRegistered(alpha)).isTrue();
            assertThat(Mixins.getConfigs()).hasSize(configsAfterRegistration);
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
