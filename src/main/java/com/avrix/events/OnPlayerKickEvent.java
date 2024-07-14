package com.avrix.events;

import zombie.core.raknet.UdpConnection;

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
     * @param connection Connection of the player who was kicked.
     * @param adminName  Nickname of the administrator who kicked the player
     * @param reason     Reason for player kick.
     */
    public abstract void handleEvent(UdpConnection connection, String adminName, String reason);
}
