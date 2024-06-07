package com.avrix.events;

import zombie.iso.IsoObject;

/**
 * Triggered when left mouse button clicked on object
 */
public abstract class OnObjectLeftMouseButtonDownEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnObjectLeftMouseButtonDown";
    }

    /**
     * Called Event Handling Method
     *
     * @param object The object on which the left mouse button was pressed down.
     * @param x      The x coordinate where the left mouse button was pressed down.
     * @param y      The y coordinate where the left mouse button was pressed down.
     */
    public abstract void handleEvent(IsoObject object, Integer x, Integer y);
}
