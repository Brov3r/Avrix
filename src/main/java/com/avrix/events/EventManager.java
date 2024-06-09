package com.avrix.events;

import com.avrix.enums.Priority;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Manages {@link Event} subscriptions and their raising.
 * Allows objects to register themselves as listeners for specific events and raise those events dynamically.
 */
public class EventManager {
    /**
     * List of all listeners for specific events, where the key is the {@link String} event name,
     * and the value is a {@link List} of all event handlers (listeners)
     */
    private static final Map<String, List<EventListener>> listeners = new HashMap<>();

    /**
     * Returns a copy of all registered listeners for all events.
     *
     * @return A copy of the listener {@link HashMap}, where the key is the event name and the value is the {@link List} of listeners for that event.
     */
    public static synchronized Map<String, List<EventListener>> getAllListeners() {
        return new HashMap<>(listeners);
    }

    /**
     * Returns a {@link List} of listeners for the specified event.
     *
     * @param eventName The name of the event for which you want to get a {@link List} of listeners.
     * @return A {@link List} of listeners for the specified event, or null if no listeners are registered for the given event.
     */
    public static synchronized List<EventListener> getListenersForEvent(String eventName) {
        return listeners.get(eventName);
    }

    /**
     * Clears all registered event listeners.
     */
    public static synchronized void clearAllListeners() {
        listeners.clear();
    }

    /**
     * Clears all registered event listeners for a specific event.
     *
     * @param eventName The name of the event to clear listeners for.
     */
    public static synchronized void clearListenersForEvent(String eventName) {
        listeners.remove(eventName);
    }

    /**
     * Registers a listener object for a specific event.
     *
     * @param listener {@link Event} listener. Must have a handleEvent method with a signature corresponding to the event.
     * @param priority {@link Priority}, events with lower priority are called last
     */
    public static synchronized void addListener(Event listener, Priority priority) {
        String eventName = listener.getEventName();
        listeners.computeIfAbsent(eventName, k -> new ArrayList<>()).add(new EventListener(listener, priority));
    }

    /**
     * Registers a listener object for a specific event. Handler priority is set to NORMAL
     *
     * @param listener {@link Event} listener. Must have a handleEvent method with a signature corresponding to the event.
     */
    public static synchronized void addListener(Event listener) {
        String eventName = listener.getEventName();
        listeners.computeIfAbsent(eventName, k -> new ArrayList<>()).add(new EventListener(listener, Priority.NORMAL));
    }

    /**
     * Raises an event by its name, passing arguments to listeners registered for that event.
     * The method dynamically looks up and calls the handleEvent method on each event listener, passing the specified arguments.
     * If an error occurs during a call, it is logged and the process continues for the remaining listeners.
     *
     * @param eventName The name of the event to raise. The event name is case insensitive.
     * @param args      Arguments to be passed to the event listener's handleEvent method. The type and number of arguments must match the expected parameters of the handleEvent method.
     */
    public static synchronized void invokeEvent(String eventName, Object... args) {
        List<EventListener> eventListeners = getListenersForEvent(eventName);

        if (eventListeners == null || eventListeners.isEmpty()) return;

        eventListeners.sort(Comparator.comparingInt(l -> l.priority().ordinal()));

        for (EventListener listener : eventListeners) {
            Event eventHandler = listener.handler();
            String argTypes = Arrays.stream(args)
                    .map(Object::getClass)
                    .map(Class::getSimpleName)
                    .collect(Collectors.joining(", "));

            try {
                invokeHandleEvent(eventHandler, args);
            } catch (NoSuchMethodException e) {
                System.out.printf("[!] Compatible 'handleEvent' method not found for event '%s' in listener '%s'. Argument types: '%s'%n",
                        eventName,
                        listener.handler.getClass(),
                        argTypes);
            } catch (IllegalAccessException | InvocationTargetException e) {
                System.out.printf("[!] An exception occurred when trying to invoke event '%s' with arguments '%s' in listener '%s'! Reason: %s%n",
                        eventName,
                        argTypes,
                        listener.handler.getClass(),
                        e.getCause() != null ? e.getCause().getMessage() : e.getMessage());
            }
        }
    }

    /**
     * Calls the handleEvent method on all registered event listeners.
     * Each listener's handleEvent method must be compatible with the arguments passed.
     *
     * @param listener The event listener to be called.
     * @param args     Arguments passed to the listener's handleEvent method.
     * @throws NoSuchMethodException if a handleEvent method with matching arguments is not found.
     */
    private static synchronized void invokeHandleEvent(Event listener, Object... args) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method[] methods = listener.getClass().getMethods();

        for (Method method : methods) {
            if (!isCompatibleMethod(method, args)) continue;

            method.invoke(listener, args);

            return;
        }

        throw new NoSuchMethodException();
    }

    /**
     * Checks whether the method meets the requirements to be called.
     * The method must be named "handleEvent" and have parameter types compatible with the arguments passed.
     *
     * @param method Method to check.
     * @param args   Arguments to be passed to the method.
     * @return true if the method meets the requirements to be called.
     */
    private static boolean isCompatibleMethod(Method method, Object[] args) {
        if (!method.getName().equals("handleEvent") || method.getParameterTypes().length != args.length) {
            return false;
        }

        for (int i = 0; i < method.getParameterTypes().length; i++) {
            if (!method.getParameterTypes()[i].isAssignableFrom(args[i].getClass())) {
                return false;
            }
        }
        return true;
    }

    /**
     * Standard event listener class
     *
     * @param handler  Handler object for this event
     * @param priority Processing priority, according to the EventPriority enumeration
     */
    public record EventListener(Event handler, Priority priority) {
    }
}
