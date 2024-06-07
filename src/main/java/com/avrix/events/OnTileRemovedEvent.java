package com.avrix.events;

import zombie.iso.IsoObject;

/**
 * Triggered when a tile object has been removed.
 */
public abstract class OnTileRemovedEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnTileRemoved";
    }

    /**
     * Called Event Handling Method
     *
     * @param object The object to be removed.
     */
    public abstract void handleEvent(IsoObject object);
}
