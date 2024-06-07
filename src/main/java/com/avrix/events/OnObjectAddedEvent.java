package com.avrix.events;

import zombie.iso.IsoObject;

/**
 * Triggered when an object is added to the map.
 */
public abstract class OnObjectAddedEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnObjectAdded";
    }

    /**
     * Called Event Handling Method
     *
     * @param object The object that was added.
     */
    public abstract void handleEvent(IsoObject object);
}
