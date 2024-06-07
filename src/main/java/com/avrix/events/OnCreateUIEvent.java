package com.avrix.events;

/**
 * Triggered after UI initialization.
 */
public abstract class OnCreateUIEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnCreateUI";
    }

    /**
     * Called Event Handling Method
     */
    public abstract void handleEvent();
}
