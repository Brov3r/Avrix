package com.avrix.events;

import zombie.characters.IsoPlayer;

/**
 * Triggered when a player is being created.
 */
public abstract class OnCreatePlayerEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnCreatePlayer";
    }

    /**
     * Called Event Handling Method
     *
     * @param playerIndex The index of the player who's being created.
     * @param player      The player who's being created.
     */
    public abstract void handleEvent(Integer playerIndex, IsoPlayer player);
}
