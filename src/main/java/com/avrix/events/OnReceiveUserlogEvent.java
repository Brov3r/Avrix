package com.avrix.events;

import java.nio.ByteBuffer;

/**
 * Triggered when the game client is receiving user log from the server.
 */
public abstract class OnReceiveUserlogEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnReceiveUserlog";
    }

    /**
     * Called Event Handling Method
     *
     * @param username The username for which we're receiving the log.
     * @param result   The resulting log.
     */
    public abstract void handleEvent(String username, ByteBuffer result);
}
