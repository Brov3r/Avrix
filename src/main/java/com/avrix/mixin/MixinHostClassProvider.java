package com.avrix.mixin;

import com.avrix.core.KnotClassLoader;
import org.spongepowered.asm.service.IClassProvider;

import java.net.URL;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Mixin class provider backed by the Avrix flat class loader.
 */
final class MixinHostClassProvider implements IClassProvider {

    private final Supplier<KnotClassLoader> classLoaderSupplier;

    /**
     * Creates a class provider that resolves the active class loader on every call.
     *
     * @param classLoaderSupplier supplier of the active flat class loader
     * @throws NullPointerException if {@code classLoaderSupplier} is null
     */
    MixinHostClassProvider(Supplier<KnotClassLoader> classLoaderSupplier) {
        this.classLoaderSupplier = Objects.requireNonNull(classLoaderSupplier, "Class loader supplier cannot be null");
    }

    /**
     * Returns the class path exposed to Mixin.
     *
     * @return registered class path URLs
     */
    @Override
    @SuppressWarnings("deprecation")
    public URL[] getClassPath() {
        KnotClassLoader classLoader = classLoaderSupplier.get();
        return classLoader == null ? new URL[0] : classLoader.getURLs();
    }

    /**
     * Loads a class through the Avrix class loader.
     *
     * @param name binary class name
     * @return loaded class
     * @throws ClassNotFoundException if the class cannot be found
     */
    @Override
    public Class<?> findClass(String name) throws ClassNotFoundException {
        return findClass(name, false);
    }

    /**
     * Loads a class through the Avrix class loader.
     *
     * @param name          binary class name
     * @param initialize    whether the class should be initialized
     * @return loaded class
     * @throws ClassNotFoundException if the class cannot be found
     */
    @Override
    public Class<?> findClass(String name, boolean initialize) throws ClassNotFoundException {
        KnotClassLoader classLoader = classLoaderSupplier.get();
        if (classLoader == null) {
            throw new ClassNotFoundException("No active class loader for: " + name);
        }
        return Class.forName(name, initialize, classLoader);
    }

    /**
     * Loads a platform agent class through the Avrix class loader.
     *
     * @param name       binary class name
     * @param initialize whether the class should be initialized
     * @return loaded class
     * @throws ClassNotFoundException if the class cannot be found
     */
    @Override
    public Class<?> findAgentClass(String name, boolean initialize) throws ClassNotFoundException {
        return findClass(name, initialize);
    }
}
