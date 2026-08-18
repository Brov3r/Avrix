package com.avrix.core;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe central service registry for the Avrix loader runtime.
 * <p>
 * Provides a lightweight dependency locator mechanism that binds unique service types
 * (interfaces or classes) to their active singleton instances across the loader lifecycle.
 *
 * @apiNote Ensures strict type safety, prevents accidental duplicate registrations,
 * and enables decoupled communication between core subsystems and external plugins.
 */
public final class ServiceManager {

    private static final Map<Class<?>, Object> SERVICES = new ConcurrentHashMap<>();

    /**
     * Prevents instantiation of this static utility class.
     *
     * @throws UnsupportedOperationException always when invoked
     */
    private ServiceManager() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Registers a singleton service implementation for the specified contract type.
     *
     * @param <T>            the compile-time service type
     * @param serviceType    the class or interface representing the service contract, cannot be null
     * @param implementation the concrete instance implementing {@code serviceType}, cannot be null
     * @throws NullPointerException     if {@code serviceType} or {@code implementation} is null
     * @throws IllegalArgumentException if {@code implementation} is not an instance of {@code serviceType}
     * @throws IllegalStateException    if a service is already registered for the specified type
     */
    public static <T> void register(Class<T> serviceType, T implementation) {
        Objects.requireNonNull(serviceType, "serviceType cannot be null");
        Objects.requireNonNull(implementation, "implementation cannot be null");

        if (!serviceType.isInstance(implementation)) {
            throw new IllegalArgumentException(
                    "Implementation [%s] is not compatible with service type [%s]"
                            .formatted(implementation.getClass().getName(), serviceType.getName())
            );
        }

        Object existing = SERVICES.putIfAbsent(serviceType, implementation);
        if (existing != null) {
            throw new IllegalStateException(
                    "Service already registered for type: " + serviceType.getName()
            );
        }
    }

    /**
     * Unregisters the service bound to the given type, if present.
     *
     * @param <T>         the service type
     * @param serviceType the class or interface to unregister, cannot be null
     * @return {@code true} if a service was bound and removed; {@code false} otherwise
     * @throws NullPointerException if {@code serviceType} is null
     */
    public static <T> boolean unregister(Class<T> serviceType) {
        Objects.requireNonNull(serviceType, "serviceType cannot be null");
        return SERVICES.remove(serviceType) != null;
    }

    /**
     * Retrieves the active service instance registered for the specified type.
     *
     * @param <T>         the expected service type
     * @param serviceType the class or interface to look up, cannot be null
     * @return the registered service instance
     * @throws NullPointerException  if {@code serviceType} is null
     * @throws IllegalStateException if no implementation has been registered for the specified type
     * @see #find(Class)
     */
    public static <T> T get(Class<T> serviceType) {
        Objects.requireNonNull(serviceType, "serviceType cannot be null");
        Object service = SERVICES.get(serviceType);

        if (service == null) {
            throw new IllegalStateException("Service not registered: " + serviceType.getName());
        }

        return serviceType.cast(service);
    }

    /**
     * Safely searches for a registered service instance without throwing an exception.
     *
     * @param <T>         the expected service type
     * @param serviceType the class or interface to look up, cannot be null
     * @return an {@link Optional} containing the service instance if present, or {@link Optional#empty()}
     * @throws NullPointerException if {@code serviceType} is null
     */
    public static <T> Optional<T> find(Class<T> serviceType) {
        Objects.requireNonNull(serviceType, "serviceType cannot be null");
        return Optional.ofNullable(serviceType.cast(SERVICES.get(serviceType)));
    }

    /**
     * Checks whether an active service implementation is registered for the specified type.
     *
     * @param serviceType the class or interface to query, cannot be null
     * @return {@code true} if a service is registered; {@code false} otherwise
     * @throws NullPointerException if {@code serviceType} is null
     */
    public static boolean contains(Class<?> serviceType) {
        Objects.requireNonNull(serviceType, "serviceType cannot be null");
        return SERVICES.containsKey(serviceType);
    }

    /**
     * Returns an unmodifiable set of all currently registered service types.
     *
     * @return unmodifiable view of registered service {@link Class} keys
     */
    public static Set<Class<?>> getRegisteredTypes() {
        return Collections.unmodifiableSet(SERVICES.keySet());
    }

    /**
     * Clears all registered services from the registry.
     *
     * @apiNote Intended exclusively for integration test cleanup and environment resets.
     */
    public static void clear() {
        SERVICES.clear();
    }
}