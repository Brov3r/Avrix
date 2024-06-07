package com.avrix.events;

/**
 * Triggered after GlobalModData has been initialized.
 */
public abstract class OnInitGlobalModDataEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnInitGlobalModData";
    }

    /**
     * Called Event Handling Method
     *
     * @param isNewGame Whether this is a new game or not.
     */
    public abstract void handleEvent(Boolean isNewGame);
}
