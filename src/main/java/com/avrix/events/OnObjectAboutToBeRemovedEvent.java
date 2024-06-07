package com.avrix.events;

import zombie.iso.IsoObject;

/**
 * Triggered when an object is about to get removed.
 */
public abstract class OnObjectAboutToBeRemovedEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnObjectAboutToBeRemoved";
    }

    /**
     * Called Event Handling Method
     *
     * @param object The object about to be removed.
     */
    public abstract void handleEvent(IsoObject object);
}
