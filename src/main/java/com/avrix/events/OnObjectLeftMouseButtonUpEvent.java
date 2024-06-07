package com.avrix.events;

import zombie.iso.IsoObject;

/**
 * Triggered when left mouse button is released on object
 */
public abstract class OnObjectLeftMouseButtonUpEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnObjectLeftMouseButtonUp";
    }

    /**
     * Called Event Handling Method
     *
     * @param object The object on which the left mouse button was released.
     * @param x      The x coordinate where the left mouse button was released.
     * @param y      The y coordinate where the left mouse button was released.
     */
    public abstract void handleEvent(IsoObject object, Integer x, Integer y);
}
