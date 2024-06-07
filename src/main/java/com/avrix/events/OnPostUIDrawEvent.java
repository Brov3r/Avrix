package com.avrix.events;

/**
 * Triggered by UI Manager in its render function after the UI has been drawn.
 */
public abstract class OnPostUIDrawEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnPostUIDraw";
    }

    /**
     * Called Event Handling Method
     */
    public abstract void handleEvent();
}
