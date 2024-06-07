package com.avrix.plugin;

import java.util.HashMap;
import java.util.Map;

/**
 * The service manager allows you to register services by their interfaces and access them
 */
public class ServiceManager {
    /**
     * Stores registered services by associating interfaces with their implementations.
     */
    private static final Map<Class<?>, Object> services = new HashMap<>();

    /**
     * Registers a service by its interface.
     *
     * @param <T>                   service type
     * @param serviceInterface      service interface
     * @param serviceImplementation service implementation
     */
    public static synchronized <T> void register(Class<T> serviceInterface, T serviceImplementation) {
        services.put(serviceInterface, serviceImplementation);
    }

    /**
     * Removes a service from the list of registered ones
     *
     * @param serviceInterface service interface
     * @param <T>              service type
     */
    public static synchronized <T> void unregister(Class<T> serviceInterface) {
        if (services.get(serviceInterface) == null) return;
        services.remove(serviceInterface);
    }

    /**
     * Returns the registered service by its interface.
     *
     * @param <T>              service type
     * @param serviceInterface service interface
     * @return an instance of the service corresponding to the specified interface, or {@code null} if the service is not found
     */
    public static synchronized <T> T getService(Class<T> serviceInterface) {
        return serviceInterface.cast(services.get(serviceInterface));
    }
}