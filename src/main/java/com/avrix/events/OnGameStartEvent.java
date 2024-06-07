package com.avrix.events;

/**
 * Triggered after the start of a new game, and after a saved game has been loaded.
 */
public abstract class OnGameStartEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnGameStart";
    }

    /**
     * Called Event Handling Method
     */
    public abstract void handleEvent();
}
