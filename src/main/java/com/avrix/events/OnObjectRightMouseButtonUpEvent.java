package com.avrix.events;

import zombie.iso.IsoObject;

/**
 * Triggered when right mouse button is released on object
 */
public abstract class OnObjectRightMouseButtonUpEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnObjectRightMouseButtonUp";
    }

    /**
     * Called Event Handling Method
     *
     * @param object The object on which the right mouse button was released.
     * @param x      The x coordinate where the right mouse button was released.
     * @param y      The x coordinate where the right mouse button was released.
     */
    public abstract void handleEvent(IsoObject object, Integer x, Integer y);
}
