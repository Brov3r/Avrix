package com.avrix.events;

import zombie.core.raknet.UdpConnection;

import java.nio.ByteBuffer;

/**
 * Triggered when the player connects to the server
 */
public abstract class OnPlayerConnectEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "onPlayerConnect";
    }

    /**
     * Called Event Handling Method
     *
     * @param data             event-related data represented as a ByteBuffer
     * @param playerConnection active player connection
     * @param username         user name
     */
    public abstract void handleEvent(ByteBuffer data, UdpConnection playerConnection, String username);
}
