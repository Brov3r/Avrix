package com.avrix.api.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages service implementations by their interfaces in a thread-safe registry.
 */
public final class ServiceManager {
    private static final Logger log = LoggerFactory.getLogger(ServiceManager.class);
    private static final Map<Class<?>, Object> services = new ConcurrentHashMap<>();

    private ServiceManager() {
        // Utility class: prevent instantiation
    }

    /**
     * Registers a service implementation for the specified interface.
     *
     * @param serviceInterface      the service interface
     * @param serviceImplementation the implementation instance
     * @param <T>                   the service type
     * @throws IllegalArgumentException if the implementation does not implement the interface
     */
    public static <T> void register(Class<T> serviceInterface, T serviceImplementation) {
        Objects.requireNonNull(serviceImplementation, "serviceImplementation cannot be null");
        if (!serviceInterface.isInstance(serviceImplementation)) {
            throw new IllegalArgumentException(
                    "Implementation [" + serviceImplementation.getClass().getName() +
                            "] does not implement interface [" + serviceInterface.getName() + "]"
            );
        }
        services.put(serviceInterface, serviceImplementation);
    }

    /**
     * Removes a previously registered service.
     *
     * @param serviceInterface the service interface to unregister
     * @param <T>              the service type
     */
    public static <T> void unregister(Class<T> serviceInterface) {
        services.remove(serviceInterface);
    }

    /**
     * Retrieves the registered service for the specified interface.
     *
     * @param serviceInterface the service interface
     * @param <T>              the service type
     * @return an {@link Optional} containing the service instance, or {@code Optional.empty()} if not found
     */
    public static <T> Optional<T> getService(Class<T> serviceInterface) {
        return Optional.ofNullable(serviceInterface.cast(services.get(serviceInterface)));
    }
}