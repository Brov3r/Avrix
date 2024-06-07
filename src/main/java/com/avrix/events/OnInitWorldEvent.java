package com.avrix.events;

/**
 * Triggered during the process of initializing the world.
 */
public abstract class OnInitWorldEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnInitWorld";
    }

    /**
     * Called Event Handling Method
     */
    public abstract void handleEvent();
}
