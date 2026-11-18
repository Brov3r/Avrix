package com.avrix.mixin;

import com.avrix.core.KnotClassLoader;
import org.spongepowered.asm.service.IClassTracker;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Tracks class loading state for the Mixin subsystem.
 */
final class MixinHostClassTracker implements IClassTracker {

    private final Supplier<KnotClassLoader> classLoaderSupplier;
    private final Set<String> invalidClasses = ConcurrentHashMap.newKeySet();

    /**
     * Creates a class tracker that resolves the active class loader on every call.
     *
     * @param classLoaderSupplier supplier of the active flat class loader
     * @throws NullPointerException if {@code classLoaderSupplier} is null
     */
    MixinHostClassTracker(Supplier<KnotClassLoader> classLoaderSupplier) {
        this.classLoaderSupplier = Objects.requireNonNull(classLoaderSupplier, "Class loader supplier cannot be null");
    }

    /**
     * Records a class that Mixin could not transform.
     *
     * @param className binary class name
     */
    @Override
    public void registerInvalidClass(String className) {
        if (className != null) {
            invalidClasses.add(className);
        }
    }

    /**
     * Checks whether a class has already been defined by the active class loader.
     *
     * @param className binary class name
     * @return {@code true} if the class is defined locally
     */
    @Override
    public boolean isClassLoaded(String className) {
        if (className == null) {
            return false;
        }
        KnotClassLoader classLoader = classLoaderSupplier.get();
        return classLoader != null && classLoader.isClassLoaded(className);
    }

    /**
     * Returns restrictions that apply to loading a class.
     *
     * @param className binary class name
     * @return empty string when the class is not restricted
     */
    @Override
    public String getClassRestrictions(String className) {
        return invalidClasses.contains(className) ? "Class is invalid for mixin transformation" : "";
    }
}
