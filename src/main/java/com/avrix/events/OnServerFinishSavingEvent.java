package com.avrix.events;

/**
 * Triggered when the server is done saving.
 */
public abstract class OnServerFinishSavingEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnServerFinishSaving";
    }

    /**
     * Called Event Handling Method
     */
    public abstract void handleEvent();
}
