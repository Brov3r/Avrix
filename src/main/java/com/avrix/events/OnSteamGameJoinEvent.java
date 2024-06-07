package com.avrix.events;

/**
 * Triggered when joining a game through Steam.
 */
public abstract class OnSteamGameJoinEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnSteamGameJoin";
    }

    /**
     * Called Event Handling Method
     */
    public abstract void handleEvent();
}
