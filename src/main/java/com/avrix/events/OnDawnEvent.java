package com.avrix.events;

/**
 * Triggered at dawn.
 */
public abstract class OnDawnEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnDawn";
    }

    /**
     * Called Event Handling Method
     */
    public abstract void handleEvent();
}
