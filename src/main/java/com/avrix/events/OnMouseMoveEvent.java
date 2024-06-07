package com.avrix.events;

/**
 * Triggered when the mouse is moved.
 */
public abstract class OnMouseMoveEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnMouseMove";
    }

    /**
     * Called Event Handling Method
     *
     * @param x  The x coordinate of the mouse position.
     * @param y  The y coordinate of the mouse position.
     * @param dx TODO: Mouse position deltaX?
     * @param dy TODO: Mouse position deltaY?
     */
    public abstract void handleEvent(Integer x, Integer y, Integer dx, Integer dy);
}
