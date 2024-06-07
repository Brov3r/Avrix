package com.avrix.events;

/**
 * Triggered when the player successfully connects to the server.
 */
public abstract class OnConnectedEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnConnected";
    }

    /**
     * Called Event Handling Method
     */
    public abstract void handleEvent();
}
