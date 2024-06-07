package com.avrix.events;

import zombie.iso.objects.IsoFire;

/**
 * Triggered when a fire starts.
 */
public abstract class OnNewFireEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnNewFire";
    }

    /**
     * Called Event Handling Method
     *
     * @param fire The fire object.
     */
    public abstract void handleEvent(IsoFire fire);
}
