package com.avrix.events;

import zombie.core.raknet.UdpConnection;

/**
 * Triggered when the player sends a command to the chat.
 */
public abstract class OnSendChatCommandEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "onSendChatCommand";
    }

    /**
     * Called Event Handling Method
     *
     * @param playerConnection Active player connection
     * @param command          command sent to the console including arguments, i.e. the entire string sent to the console
     */
    public abstract void handleEvent(UdpConnection playerConnection, String command);
}
