package com.avrix.api.events;

import zombie.characters.IsoPlayer;

/**
 * Triggered when a player dies.
 */
public abstract class OnPlayerDeathEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnPlayerDeath";
    }

    /**
     * Called Event Handling Method
     *
     * @param player The player who's about to die.
     */
    public abstract void handle(IsoPlayer player);
}
