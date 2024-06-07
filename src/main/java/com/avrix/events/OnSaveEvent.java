package com.avrix.events;

/**
 * Triggered during the game's saving process.
 */
public abstract class OnSaveEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnSave";
    }

    /**
     * Called Event Handling Method
     */
    public abstract void handleEvent();
}
