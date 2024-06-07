package com.avrix.events;

/**
 * Triggered before a map is loaded.
 */
public abstract class OnPreMapLoadEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnPreMapLoad";
    }

    /**
     * Called Event Handling Method
     */
    public abstract void handleEvent();
}
