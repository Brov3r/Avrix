package com.avrix.events;

/**
 * Triggered when the game client receives statistics from the server.
 */
public abstract class OnServerStatisticReceivedEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnServerStatisticReceived";
    }

    /**
     * Called Event Handling Method
     */
    public abstract void handleEvent();
}
