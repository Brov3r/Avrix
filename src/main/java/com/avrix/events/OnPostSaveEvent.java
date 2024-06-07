package com.avrix.events;

/**
 * Triggered after a game has been saved.
 */
public abstract class OnPostSaveEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnPostSave";
    }

    /**
     * Called Event Handling Method
     */
    public abstract void handleEvent();
}
