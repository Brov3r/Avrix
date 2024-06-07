package com.avrix.events;

/**
 * Triggered when either a game or a server is being started, or when mods are getting reloaded during a game.
 */
public abstract class OnGameBootEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnGameBoot";
    }

    /**
     * Called Event Handling Method
     */
    public abstract void handleEvent();
}
