package com.avrix.events;

import zombie.iso.IsoObject;

/**
 * Triggered when right mouse button clicked on object
 */
public abstract class OnObjectRightMouseButtonDownEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnObjectRightMouseButtonDown";
    }

    /**
     * Called Event Handling Method
     *
     * @param object The object on which the right mouse button was pressed down.
     * @param x      The x coordinate where the right mouse button was pressed down.
     * @param y      The y coordinate where the right mouse button was pressed down.
     */
    public abstract void handleEvent(IsoObject object, Integer x, Integer y);
}
