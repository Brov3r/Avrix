package com.avrix.events;

/**
 * Triggered when the server is fully initialized.
 */
public abstract class OnServerInitializeEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "onServerInitialize";
    }

    /**
     * Called Event Handling Method
     */
    public abstract void handleEvent();
}
