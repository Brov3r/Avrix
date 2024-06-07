package com.avrix.events;

import zombie.characters.IsoPlayer;

/**
 * Triggered when a player is being updated.
 */
public abstract class OnPlayerUpdateEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnPlayerUpdate";
    }

    /**
     * Called Event Handling Method
     *
     * @param player The player who's being updated.
     */
    public abstract void handleEvent(IsoPlayer player);
}
