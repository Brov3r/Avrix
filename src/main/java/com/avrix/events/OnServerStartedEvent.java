package com.avrix.events;

/**
 * Triggered when the game server has started.
 */
public abstract class OnServerStartedEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnServerStarted";
    }

    /**
     * Called Event Handling Method
     */
    public abstract void handleEvent();
}
