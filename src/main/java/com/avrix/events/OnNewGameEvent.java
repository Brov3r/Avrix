package com.avrix.events;

import zombie.characters.IsoPlayer;
import zombie.iso.IsoGridSquare;

/**
 * Triggered after a new world has been initialized.
 */
public abstract class OnNewGameEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnNewGame";
    }

    /**
     * Called Event Handling Method
     *
     * @param player The player who's starting the game.
     * @param square The grid square where the player is located.
     */
    public abstract void handleEvent(IsoPlayer player, IsoGridSquare square);
}
