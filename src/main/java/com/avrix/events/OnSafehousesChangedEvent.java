package com.avrix.events;

/**
 * Triggered when the safehouse is being changed.
 */
public abstract class OnSafehousesChangedEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnSafehousesChanged";
    }

    /**
     * Called Event Handling Method
     */
    public abstract void handleEvent();
}
