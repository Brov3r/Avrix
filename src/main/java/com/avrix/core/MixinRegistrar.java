package com.avrix.core;

import java.util.Collection;

/**
 * Registers mixin classes that should be applied during class loading.
 */
public interface MixinRegistrar {

    /**
     * Registers a single mixin class.
     *
     * @param mixinClass fully qualified mixin class name
     */
    void addMixin(String mixinClass);

    /**
     * Registers a single mixin class owned by a plugin.
     *
     * <p>The owner is the canonical plugin identifier from the plugin manifest. It is attached to the generated
     * Mixin configuration as diagnostic metadata so Mixin messages identify the owning plugin. It is not a Workshop
     * item identifier: Workshop IDs describe the containing distribution package and may cover several plugins.
     *
     * <p>Registrars without owner support ignore the owner and delegate to {@link #addMixin(String)}.
     *
     * @param mixinClass fully qualified mixin class name
     * @param ownerId    canonical plugin identifier that owns the mixin, or {@code null} when unknown
     */
    default void addMixin(String mixinClass, String ownerId) {
        addMixin(mixinClass);
    }

    /**
     * Checks a plugin's mixin declarations without changing the active transformer state.
     * <p>
     * The default does nothing for registrars without preflight support. Registration failures must still be
     * treated as potentially leaving partial state behind.
     *
     * @param mixinClasses fully qualified mixin class names
     * @throws RuntimeException if a declaration cannot be registered
     */
    default void validateMixins(Collection<String> mixinClasses) {
    }
}
