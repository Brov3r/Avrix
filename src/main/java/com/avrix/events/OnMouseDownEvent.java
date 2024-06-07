package com.avrix.events;

/**
 * Triggered when the mouse button is down.
 */
public abstract class OnMouseDownEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnMouseDown";
    }

    /**
     * Called Event Handling Method
     *
     * @param x The x coordinate where the mouse button was pressed down.
     * @param y The y coordinate where the mouse button was pressed down.
     */
    public abstract void handleEvent(Integer x, Integer y);
}
