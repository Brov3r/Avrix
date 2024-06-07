package com.avrix.events;

import zombie.characters.IsoPlayer;
import zombie.core.raknet.UdpConnection;

/**
 * Triggered when the server decided to disconnect from the player.
 */
public abstract class OnPlayerDisconnectEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "onPlayerDisconnect";
    }

    /**
     * Called Event Handling Method
     *
     * @param player           Object of the player who left.
     * @param playerConnection Connection of a player who has left the server.
     */
    public abstract void handleEvent(IsoPlayer player, UdpConnection playerConnection);
}
