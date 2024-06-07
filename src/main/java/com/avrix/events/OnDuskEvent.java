package com.avrix.events;

/**
 * Triggered at dusk.
 */
public abstract class OnDuskEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnDusk";
    }

    /**
     * Called Event Handling Method
     */
    public abstract void handleEvent();
}
