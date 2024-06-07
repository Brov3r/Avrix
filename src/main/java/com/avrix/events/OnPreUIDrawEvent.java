package com.avrix.events;

/**
 * Triggered by UI Manager in its render function before the UI gets drawn.
 */
public abstract class OnPreUIDrawEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnPreUIDraw";
    }

    /**
     * Called Event Handling Method
     */
    public abstract void handleEvent();
}
