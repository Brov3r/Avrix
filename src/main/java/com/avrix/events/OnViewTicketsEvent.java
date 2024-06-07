package com.avrix.events;

import zombie.core.network.ByteBufferWriter;

/**
 * Triggered when the game client is receiving tickets from the server.
 */
public abstract class OnViewTicketsEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "ViewTickets";
    }

    /**
     * Called Event Handling Method
     *
     * @param tickets The buffer where to write the tickets.
     */
    public abstract void handleEvent(ByteBufferWriter tickets);
}
