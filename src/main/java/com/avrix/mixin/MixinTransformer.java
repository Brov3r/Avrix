package com.avrix.mixin;

import com.avrix.core.KnotClassLoader;
import net.lenni0451.classtransform.TransformerManager;
import net.lenni0451.classtransform.mixinstranslator.MixinsTranslator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Objects;

/**
 * Central facade managing bytecode transformations and mixin registrations in the Avrix loader.
 * <p>
 * Encapsulates the {@link TransformerManager} and configures the {@link MixinsTranslator} preprocessor
 * to allow Sponge-style mixin annotations to run on top of ClassTransform.
 *
 * @apiNote Must be initialized via {@link #init(KnotClassLoader)} before any game classes are loaded.
 */
public final class MixinTransformer {

    private static final Logger LOGGER = LoggerFactory.getLogger(MixinTransformer.class);

    private static volatile TransformerManager manager;
    private static volatile boolean initialized = false;

    /**
     * Prevents direct instantiation of this utility facade.
     */
    private MixinTransformer() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Initializes the mixin transformation subsystem with the provided {@link KnotClassLoader}.
     *
     * @param knotClassLoader the flat classloader containing game classes, dependencies, and plugins
     * @throws NullPointerException if {@code knotClassLoader} is null
     */
    public static synchronized void init(KnotClassLoader knotClassLoader) {
        Objects.requireNonNull(knotClassLoader, "KnotClassLoader cannot be null");

        if (initialized) {
            LOGGER.warn("Mixin environment is already initialized. Skipping re-initialization.");
            return;
        }

        LOGGER.info("Initializing Avrix Mixin environment...");

        CompositeClassProvider classProvider = new CompositeClassProvider(knotClassLoader);
        TransformerManager transformerManager = new TransformerManager(classProvider);

        // Translate SpongePowered Mixin annotations into ClassTransform format
        transformerManager.addTransformerPreprocessor(new MixinsTranslator());

        manager = transformerManager;
        initialized = true;

        LOGGER.info("Mixin environment initialized successfully.");
    }

    /**
     * Returns the active {@link TransformerManager} instance.
     *
     * @return the initialized transformer manager
     * @throws IllegalStateException if the environment has not been initialized yet
     */
    public static TransformerManager getManager() {
        if (!initialized || manager == null) {
            throw new IllegalStateException("MixinTransformer is not initialized. Call MixinTransformer.init() first.");
        }
        return manager;
    }

    /**
     * Registers a single mixin transformer by its fully qualified class name.
     *
     * @param mixinClass the fully qualified class name of the mixin (e.g., {@code com.avrix.plugin.mixin.PlayerMixin})
     * @throws NullPointerException     if {@code mixinClass} is null
     * @throws IllegalArgumentException if {@code mixinClass} is blank
     * @throws IllegalStateException    if the mixin environment is not initialized
     */
    public static synchronized void addMixin(String mixinClass) {
        Objects.requireNonNull(mixinClass, "mixinClass cannot be null");

        if (mixinClass.isBlank()) {
            throw new IllegalArgumentException("Mixin class name cannot be blank");
        }

        getManager().addTransformer(mixinClass);
        LOGGER.debug("Registered mixin transformer: [{}]", mixinClass);
    }

    /**
     * Registers a collection of mixin transformer classes.
     *
     * @param mixins collection of fully qualified mixin class names
     * @throws NullPointerException  if {@code mixins} or any of its elements is null
     * @throws IllegalStateException if the mixin environment is not initialized
     */
    public static synchronized void addMixins(Collection<String> mixins) {
        Objects.requireNonNull(mixins, "Mixins collection cannot be null");

        for (String mixin : mixins) {
            addMixin(mixin);
        }
    }

    /**
     * Checks whether the mixin subsystem has been initialized.
     *
     * @return {@code true} if initialized, {@code false} otherwise
     */
    public static boolean isInitialized() {
        return initialized;
    }

    /**
     * Resets the internal state of the mixin environment.
     *
     * @apiNote Used primarily in integration testing and context reloads.
     */
    public static synchronized void reset() {
        manager = null;
        initialized = false;
        LOGGER.debug("Mixin environment has been reset.");
    }
}