package com.avrix.events;

import zombie.characters.IsoPlayer;

/**
 * Triggered when the player kick command is called.
 */
public abstract class OnPlayerKickEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "onPlayerKick";
    }

    /**
     * Called Event Handling Method
     *
     * @param player    An object of the player who was banned.
     * @param adminName Nickname of the administrator who kicked the player
     * @param reason    Reason for blocking the player.
     */
    public abstract void handleEvent(IsoPlayer player, String adminName, String reason);
}
