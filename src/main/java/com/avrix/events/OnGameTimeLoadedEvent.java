package com.avrix.events;

/**
 * Triggered after the Game time has been initialized.
 */
public abstract class OnGameTimeLoadedEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnGameTimeLoaded";
    }

    /**
     * Called Event Handling Method
     */
    public abstract void handleEvent();
}
