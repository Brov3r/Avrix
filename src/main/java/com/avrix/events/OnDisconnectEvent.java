package com.avrix.events;

/**
 * Triggered when the player disconnects from the server.
 */
public abstract class OnDisconnectEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnDisconnect";
    }

    /**
     * Called Event Handling Method
     */
    public abstract void handleEvent();
}
