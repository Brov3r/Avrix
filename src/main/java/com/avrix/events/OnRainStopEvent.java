package com.avrix.events;

/**
 * Triggered when it stops raining.
 */
public abstract class OnRainStopEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnRainStop";
    }

    /**
     * Called Event Handling Method
     */
    public abstract void handleEvent();
}
