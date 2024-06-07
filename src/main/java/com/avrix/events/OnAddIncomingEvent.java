package com.avrix.events;

import zombie.core.raknet.UdpConnection;

import java.nio.ByteBuffer;

/**
 * Triggered when a packet from a client arrives at the server.
 */
public abstract class OnAddIncomingEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "onAddIncoming";
    }

    /**
     * Called Event Handling Method
     *
     * @param opcode           an opcode that specifies the type of event
     * @param data             data associated with a packet, represented as a ByteBuffer
     * @param playerConnection player connection associated with packet
     */
    public abstract void handleEvent(Short opcode, ByteBuffer data, UdpConnection playerConnection);
}
