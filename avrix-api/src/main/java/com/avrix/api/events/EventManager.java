package com.avrix.api.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * High-performance, thread-safe event dispatcher using priority-based listener invocation.
 *
 * <p>Listeners are registered per event name and invoked in descending priority order.
 * Handler method resolution is cached at registration time to minimize reflection overhead.
 * All operations are lock-free using concurrent collections and copy-on-write semantics.
 */
public final class EventManager {
    private static final Logger log = LoggerFactory.getLogger(EventManager.class);

    private static final Map<String, CopyOnWriteArrayList<EventListener>> listeners = new ConcurrentHashMap<>();
    private static final Map<MethodCacheKey, List<Method>> methodCache = new ConcurrentHashMap<>();

    private EventManager() {
        // Utility class: prevent instantiation
    }

    /**
     * Returns an unmodifiable snapshot of all registered listeners grouped by event name.
     *
     * @return a thread-safe, unmodifiable map containing event names and their listener lists
     * @throws UnsupportedOperationException if the caller attempts to modify the returned map or lists
     */
    public static Map<String, List<EventListener>> getAllListeners() {
        return listeners.entrySet().stream()
                .collect(Collectors.toUnmodifiableMap(
                        Map.Entry::getKey,
                        entry -> List.copyOf(entry.getValue())
                ));
    }

    /**
     * Returns an unmodifiable snapshot of listeners for the specified event.
     *
     * @param eventName the name of the event to query
     * @return an unmodifiable list of listeners, or an empty list if none registered
     * @throws IllegalArgumentException if eventName is null
     */
    public static List<EventListener> getListenersForEvent(String eventName) {
        Objects.requireNonNull(eventName, "eventName must not be null");
        return Optional.ofNullable(listeners.get(eventName))
                .map(List::copyOf)
                .orElse(Collections.emptyList());
    }

    /**
     * Removes all registered listeners and clears the method resolution cache.
     */
    public static void clearAllListeners() {
        listeners.clear();
        methodCache.clear();
    }

    /**
     * Removes all listeners registered for the specified event name.
     *
     * @param eventName the event name to clear listeners for
     * @throws IllegalArgumentException if eventName is null
     */
    public static void clearListenersForEvent(String eventName) {
        Objects.requireNonNull(eventName, "eventName must not be null");
        listeners.remove(eventName);
    }

    /**
     * Registers a listener with default priority (0).
     *
     * @param listener the event listener instance to register
     * @throws NullPointerException     if listener is null
     * @throws IllegalArgumentException if no compatible {@code handle} method is found
     */
    public static void addListener(Event listener) {
        addListener(listener, 0);
    }

    /**
     * Registers a listener with the specified priority.
     *
     * <p>Listeners with higher priority values are invoked first. Method resolution
     * is performed once and cached for subsequent invocations.
     *
     * @param listener the event listener instance to register
     * @param priority the execution priority (higher = earlier invocation)
     * @throws NullPointerException     if listener is null
     * @throws IllegalArgumentException if no compatible {@code handle} method is found
     */
    public static void addListener(Event listener, int priority) {
        Objects.requireNonNull(listener, "listener must not be null");
        String eventName = listener.getEventName().toLowerCase(Locale.ROOT);

        List<Method> methods = resolveAndCacheHandlerMethods(listener.getClass(), eventName);
        EventListener eventListener = new EventListener(listener, priority, methods);

        CopyOnWriteArrayList<EventListener> list = listeners.computeIfAbsent(eventName, k -> new CopyOnWriteArrayList<>());
        insertSorted(list, eventListener);
    }

    /**
     * Inserts a listener into the list maintaining descending priority order.
     * Uses stable insertion sort — optimal for typically small listener lists.
     *
     * @param list    the concurrent list to insert into
     * @param element the listener element to insert
     */
    private static void insertSorted(CopyOnWriteArrayList<EventListener> list, EventListener element) {
        int i = list.size() - 1;
        while (i >= 0 && list.get(i).priority() < element.priority()) {
            i--;
        }
        list.add(i + 1, element);
    }

    /**
     * Unregisters a previously registered listener.
     *
     * @param listener the listener instance to remove
     * @return {@code true} if the listener was found and removed, {@code false} otherwise
     * @throws NullPointerException if listener is null
     */
    public static boolean removeListener(Event listener) {
        Objects.requireNonNull(listener, "listener must not be null");
        String eventName = listener.getEventName().toLowerCase(Locale.ROOT);
        CopyOnWriteArrayList<EventListener> list = listeners.get(eventName);

        if (list == null) return false;

        boolean removed = list.removeIf(el -> el.handler().equals(listener));

        if (removed && list.isEmpty()) {
            listeners.remove(eventName, list);
        }
        return removed;
    }

    /**
     * Invokes all registered listeners for the specified event name.
     *
     * <p>Listeners are executed in descending priority order. Each listener's first compatible
     * {@code handle} method matching the runtime argument types is invoked. Primitive/wrapper
     * autoboxing is supported. Exceptions in individual handlers are logged and do not stop
     * the invocation chain.
     *
     * @param eventName the name of the event to dispatch
     * @param args      arguments to pass to each listener's handler method
     * @throws IllegalArgumentException if eventName is null
     */
    public static void invoke(String eventName, Object... args) {
        Objects.requireNonNull(eventName, "eventName must not be null");
        CopyOnWriteArrayList<EventListener> eventListeners = listeners.get(eventName.toLowerCase(Locale.ROOT));
        if (eventListeners == null || eventListeners.isEmpty()) return;

        for (EventListener listener : eventListeners) {
            boolean invoked = false;

            for (Method method : listener.handleMethods()) {
                if (isArgumentsAssignable(method.getParameterTypes(), args)) {
                    try {
                        method.invoke(listener.handler(), args);
                        invoked = true;
                        break;
                    } catch (IllegalAccessException e) {
                        log.error("Access denied to '{}' in listener '{}'", method.getName(), listener.handler().getClass().getSimpleName(), e);
                    } catch (InvocationTargetException e) {
                        log.error("Exception in '{}' in listener '{}': {}", method.getName(), listener.handler().getClass().getSimpleName(), e.getTargetException().getMessage(), e);
                    }
                }
            }

            if (!invoked) {
                log.warn("Skipping listener '{}': no compatible 'handle' method for args [{}]",
                        listener.handler().getClass().getSimpleName(),
                        Arrays.stream(args).map(a -> a == null ? "null" : a.getClass().getSimpleName()).collect(Collectors.joining(", ")));
            }
        }
    }

    /**
     * Resolves and caches all {@code handle} methods for a listener class.
     *
     * @param listenerClass the class of the listener instance
     * @param eventName     the associated event name (used for cache key generation)
     * @return an unmodifiable list of resolved handler methods with accessibility enabled
     * @throws IllegalArgumentException if no methods named {@code handle} are found
     */
    private static List<Method> resolveAndCacheHandlerMethods(Class<?> listenerClass, String eventName) {
        return methodCache.computeIfAbsent(new MethodCacheKey(listenerClass, eventName.toLowerCase(Locale.ROOT)), k ->
                Arrays.stream(listenerClass.getMethods())
                        .filter(m -> "handle".equals(m.getName()))
                        .peek(m -> m.setAccessible(true))
                        .toList()
        );
    }

    /**
     * Checks if method parameter types are assignable from runtime argument types.
     *
     * @param paramTypes the declared parameter types of the method
     * @param args       the runtime arguments to validate
     * @return {@code true} if all arguments are assignable to their corresponding parameters
     */
    private static boolean isArgumentsAssignable(Class<?>[] paramTypes, Object[] args) {
        if (paramTypes.length != args.length) return false;
        for (int i = 0; i < paramTypes.length; i++) {
            if (!isAssignable(paramTypes[i], args[i])) return false;
        }
        return true;
    }

    /**
     * Determines if a runtime argument is assignable to a declared parameter type,
     * handling nulls and primitive/wrapper conversions.
     *
     * @param paramType the declared parameter type
     * @param arg       the runtime argument (may be {@code null})
     * @return {@code true} if assignable
     */
    private static boolean isAssignable(Class<?> paramType, Object arg) {
        if (arg == null) return !paramType.isPrimitive();
        Class<?> argType = arg.getClass();
        if (paramType.isAssignableFrom(argType)) return true;
        if (paramType.isPrimitive()) return isPrimitiveWrapperMatch(paramType, argType);
        if (argType.isPrimitive()) return isPrimitiveWrapperMatch(argType, paramType);
        return false;
    }

    /**
     * Checks if a primitive type and a class are matching wrapper/primitive pairs.
     *
     * @param primitive the primitive type (e.g., {@code int.class})
     * @param wrapper   the potential wrapper class (e.g., {@code Integer.class})
     * @return {@code true} if they are a matching pair
     */
    private static boolean isPrimitiveWrapperMatch(Class<?> primitive, Class<?> wrapper) {
        return switch (primitive.getName()) {
            case "int" -> wrapper == Integer.class;
            case "long" -> wrapper == Long.class;
            case "boolean" -> wrapper == Boolean.class;
            case "byte" -> wrapper == Byte.class;
            case "char" -> wrapper == Character.class;
            case "short" -> wrapper == Short.class;
            case "float" -> wrapper == Float.class;
            case "double" -> wrapper == Double.class;
            case "void" -> wrapper == Void.class;
            default -> false;
        };
    }

    /**
     * Immutable cache key for method resolution: listener class + event name.
     *
     * @param listenerClass the listener class
     * @param eventName     the event name
     */
    private record MethodCacheKey(Class<?> listenerClass, String eventName) {
    }

    /**
     * Immutable record representing a registered event listener.
     *
     * @param handler       the listener instance
     * @param priority      execution priority (higher values execute first)
     * @param handleMethods cached list of {@code handle} methods for dynamic dispatch
     */
    public record EventListener(Event handler, int priority, List<Method> handleMethods) {
        /**
         * Compact constructor enforcing validity constraints.
         *
         * @throws NullPointerException     if handler or handleMethods is null
         * @throws IllegalArgumentException if handleMethods is empty
         */
        public EventListener {
            Objects.requireNonNull(handler, "handler must not be null");
            Objects.requireNonNull(handleMethods, "handleMethods must not be null");
            if (handleMethods.isEmpty()) {
                throw new IllegalArgumentException("No 'handle' methods found in " + handler.getClass());
            }
        }
    }
}