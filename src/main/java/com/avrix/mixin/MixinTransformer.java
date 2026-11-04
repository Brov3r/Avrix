package com.avrix.mixin;

import com.avrix.core.ClassTransformer;
import com.avrix.core.KnotClassLoader;
import com.avrix.core.MixinRegistrar;
import net.lenni0451.classtransform.TransformerManager;
import net.lenni0451.classtransform.mixinstranslator.MixinsTranslator;
import net.lenni0451.classtransform.utils.FailStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Objects;

/**
 * Instance-scoped mixin transformer for the Avrix loader.
 * <p>
 * Encapsulates the {@link TransformerManager} and configures the {@link MixinsTranslator} preprocessor
 * to allow Sponge-style mixin annotations to run on top of ClassTransform.
 */
public final class MixinTransformer implements ClassTransformer, MixinRegistrar {

    private static final Logger LOGGER = LoggerFactory.getLogger(MixinTransformer.class);

    private final TransformerManager manager;

    /**
     * Creates a lenient mixin transformer for the provided {@link KnotClassLoader}.
     *
     * @param knotClassLoader the flat classloader containing game classes, dependencies, and plugins
     * @throws NullPointerException if {@code knotClassLoader} is null
     */
    public MixinTransformer(KnotClassLoader knotClassLoader) {
        this(knotClassLoader, false);
    }

    /**
     * Creates a mixin transformer for the provided {@link KnotClassLoader}.
     *
     * <p>In lenient mode transformation failures cancel only the affected class and are logged. In strict mode
     * failures are propagated to the classloader so startup can be aborted by the existing {@code --strict}
     * handling.
     *
     * @param knotClassLoader the flat classloader containing game classes, dependencies, and plugins
     * @param strict          whether transformation failures should abort class loading
     * @throws NullPointerException if {@code knotClassLoader} is null
     */
    public MixinTransformer(KnotClassLoader knotClassLoader, boolean strict) {
        Objects.requireNonNull(knotClassLoader, "KnotClassLoader cannot be null");

        CompositeClassProvider classProvider = new CompositeClassProvider(knotClassLoader);
        this.manager = new TransformerManager(classProvider);
        this.manager.setFailStrategy(strict ? FailStrategy.THROW : FailStrategy.CANCEL);

        // Translate SpongePowered Mixin annotations into ClassTransform format
        this.manager.addTransformerPreprocessor(new MixinsTranslator());

        LOGGER.info("Mixin environment initialized successfully (strict={}).", strict);
    }

    /**
     * Transforms class bytecode using the active {@link TransformerManager}.
     *
     * @param className     binary name of the class
     * @param originalBytes original class bytes
     * @return transformed class bytes, or {@code null} if no transformation is applied
     */
    @Override
    public byte[] transform(String className, byte[] originalBytes) {
        return manager.transform(className, originalBytes);
    }

    /**
     * Returns the active {@link TransformerManager} instance.
     *
     * @return transformer manager owned by this instance
     */
    public TransformerManager getManager() {
        return manager;
    }

    /**
     * Registers a single mixin transformer by its fully qualified class name.
     *
     * @param mixinClass the fully qualified class name of the mixin (e.g., {@code com.avrix.plugin.mixin.PlayerMixin})
     * @throws NullPointerException     if {@code mixinClass} is null
     * @throws IllegalArgumentException if {@code mixinClass} is blank
     */
    @Override
    public synchronized void addMixin(String mixinClass) {
        Objects.requireNonNull(mixinClass, "mixinClass cannot be null");

        if (mixinClass.isBlank()) {
            throw new IllegalArgumentException("Mixin class name cannot be blank");
        }

        manager.addTransformer(mixinClass);
        LOGGER.debug("Registered mixin transformer: [{}]", mixinClass);
    }

    /**
     * Registers a collection of mixin transformer classes.
     *
     * @param mixins collection of fully qualified mixin class names
     * @throws NullPointerException if {@code mixins} or any of its elements is null
     */
    public synchronized void addMixins(Collection<String> mixins) {
        Objects.requireNonNull(mixins, "Mixins collection cannot be null");

        for (String mixin : mixins) {
            addMixin(mixin);
        }
    }
}
