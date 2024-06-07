package com.avrix.events;

/**
 * Triggered when the list of public Internet servers is being refreshed.
 */
public abstract class OnSteamRefreshInternetServersEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnSteamRefreshInternetServers";
    }

    /**
     * Called Event Handling Method
     */
    public abstract void handleEvent();
}
