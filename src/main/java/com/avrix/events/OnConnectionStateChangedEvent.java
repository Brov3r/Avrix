package com.avrix.events;

/**
 * When a player is connecting to the server, the connection is going through different stages. This event is triggered for each of these stages of the initial connection.
 */
public abstract class OnConnectionStateChangedEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnConnectionStateChanged";
    }

    /**
     * Called Event Handling Method
     *
     * @param state  The current state of the connection that has changed.
     * @param reason The reason leading to the state change. It can be null.
     */
    public abstract void handleEvent(String state, String reason);
}
