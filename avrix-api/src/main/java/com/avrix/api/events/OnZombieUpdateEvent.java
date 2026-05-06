package com.avrix.api.events;

import zombie.characters.IsoZombie;

/**
 * Triggered when a zombie is being updated.
 */
public abstract class OnZombieUpdateEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnZombieUpdate";
    }

    /**
     * Called Event Handling Method
     *
     * @param zombie The zombie who's being updated.
     */
    public abstract void handle(IsoZombie zombie);
}
