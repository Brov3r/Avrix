package com.avrix.events;

/**
 * Triggered when the connection to the server has failed.
 */
public abstract class OnConnectFailedEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnConnectFailed";
    }

    /**
     * Called Event Handling Method
     *
     * @param error The error message describing the reason for the connection failure.
     */
    public abstract void handleEvent(String error);
}
