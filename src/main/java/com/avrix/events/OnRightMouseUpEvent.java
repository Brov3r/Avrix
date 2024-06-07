package com.avrix.events;

/**
 * Triggered when mouse button is released.
 */
public abstract class OnRightMouseUpEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnRightMouseUp";
    }

    /**
     * Called Event Handling Method
     *
     * @param x The x coordinate where the right mouse button was released.
     * @param y The y coordinate where the right mouse button was released.
     */
    public abstract void handleEvent(Integer x, Integer y);
}
