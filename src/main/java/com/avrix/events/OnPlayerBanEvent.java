package com.avrix.events;

import zombie.characters.IsoPlayer;

/**
 * Triggered when the player ban command is called.
 */
public abstract class OnPlayerBanEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "onPlayerBan";
    }

    /**
     * Called Event Handling Method
     *
     * @param player    An object of the player who was banned.
     * @param adminName Nickname of the administrator who banned the player
     * @param reason    Reason for blocking the player.
     */
    public abstract void handleEvent(IsoPlayer player, String adminName, String reason);
}
