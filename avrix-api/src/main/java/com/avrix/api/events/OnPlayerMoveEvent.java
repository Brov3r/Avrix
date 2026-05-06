package com.avrix.api.events;

import zombie.characters.IsoPlayer;

/**
 * Triggered while the player is moving.
 */
public abstract class OnPlayerMoveEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnPlayerMove";
    }

    /**
     * Called Event Handling Method
     *
     * @param player The player who's moving.
     */
    public abstract void handle(IsoPlayer player);
}
