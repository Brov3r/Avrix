package com.avrix.events;

/**
 * TODO
 */
public abstract class OnSteamServerRespondedEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnSteamServerResponded";
    }

    /**
     * Called Event Handling Method
     *
     * @param serverIndex TODO
     */
    public abstract void handleEvent(Integer serverIndex);
}
