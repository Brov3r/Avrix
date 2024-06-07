package com.avrix.events;

import zombie.iso.objects.IsoThumpable;

/**
 * Triggered when a thumpable object is being destroyed.
 */
public abstract class OnDestroyIsoThumpableEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnDestroyIsoThumpable";
    }

    /**
     * Called Event Handling Method
     *
     * @param thumpable The thumpable object which is being destroyed.
     */
    public abstract void handleEvent(IsoThumpable thumpable);
}
