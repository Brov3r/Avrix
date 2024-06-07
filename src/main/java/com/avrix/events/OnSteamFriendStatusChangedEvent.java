package com.avrix.events;

/**
 * Triggered when the status of a friend changed on Steam.
 */
public abstract class OnSteamFriendStatusChangedEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnSteamFriendStatusChanged";
    }

    /**
     * Called Event Handling Method
     *
     * @param steamId Steam identifier of the user who's friend status has changed.
     */
    public abstract void handleEvent(String steamId);
}
