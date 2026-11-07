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
    public void addMixin(String mixinClass);

    /**
     * Checks a plugin's mixin declarations without changing the active transformer state.
     * <p>
     * The default does nothing for registrars without preflight support. Registration failures must still be
     * treated as potentially leaving partial state behind.
     *
     * @param mixinClasses fully qualified mixin class names
     * @throws RuntimeException if a declaration cannot be registered
     */
    public default void validateMixins(Collection<String> mixinClasses) {
    }
}
