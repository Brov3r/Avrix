package com.avrix.events;

/**
 * Triggered when the server starts saving.
 */
public abstract class OnServerStartSavingEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnServerStartSaving";
    }

    /**
     * Called Event Handling Method
     */
    public abstract void handleEvent();
}
