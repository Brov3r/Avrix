package com.avrix.events;

import zombie.characters.IsoZombie;

/**
 * Triggered when a zombie dies.
 */
public abstract class OnZombieDeadEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnZombieDead";
    }

    /**
     * Called Event Handling Method
     *
     * @param zombie The zombie who's about to get killed.
     */
    public abstract void handleEvent(IsoZombie zombie);
}
