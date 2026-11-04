package com.avrix.core;

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
}
