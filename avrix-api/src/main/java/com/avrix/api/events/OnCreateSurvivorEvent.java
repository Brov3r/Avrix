package com.avrix.api.events;

import zombie.characters.IsoSurvivor;

/**
 * Triggered when a survivor is being created.
 */
public abstract class OnCreateSurvivorEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnCreateSurvivor";
    }

    /**
     * Called Event Handling Method
     *
     * @param survivor The survivor who's being created.
     */
    public abstract void handle(IsoSurvivor survivor);
}
