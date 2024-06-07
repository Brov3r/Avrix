package com.avrix.events;

/**
 * Triggered when a game is loading, after Lua Events/OnGameStart.
 */
public abstract class OnLoadEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnLoad";
    }

    /**
     * Called Event Handling Method
     */
    public abstract void handleEvent();
}
