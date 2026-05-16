package com.avrix.api.events;

import zombie.characters.IsoZombie;

/**
 * TODO: Description
 */
public abstract class OnZombieCreateEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnZombieCreate";
    }

    /**
     * Called Event Handling Method
     *
     * @param zombie todo
     */
    public abstract void handle(IsoZombie zombie);
}
