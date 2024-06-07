package com.avrix.events;

/**
 * Triggered when the server shuts down
 */
public abstract class OnServerShutdownEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "onServerShutdown";
    }

    /**
     * Called Event Handling Method
     */
    public abstract void handleEvent();
}
