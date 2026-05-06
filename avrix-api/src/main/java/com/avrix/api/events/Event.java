package com.avrix.api.events;

/**
 * Base abstract class for all custom events in the dispatch system.
 *
 * <p>Defines the foundational contract for event identification and dynamic handler resolution.
 * Subclasses must implement {@link #getEventName()} and provide at least one {@code handle} method
 * whose signature is compatible with the runtime arguments passed to
 * {@link EventManager#invoke(String, Object...)}.
 */
public abstract class Event {

    /**
     * Returns the unique identifier for this event type.
     *
     * <p>The returned name serves as a routing key in {@link EventManager}, enabling listener
     * registration, priority dispatching, and reflective method caching. It must remain stable
     * across all instances of the same event subclass.
     *
     * @return a stable, non-null string uniquely identifying the event category, used by
     * {@link EventManager} for listener routing and handler resolution
     */
    public abstract String getEventName();
}