package com.avrix.events;

/**
 * TODO
 */
public abstract class OnSteamServerFailedToRespond2Event extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnSteamServerFailedToRespond2";
    }

    /**
     * Called Event Handling Method
     *
     * @param host TODO
     * @param port TODO
     */
    public abstract void handleEvent(String host, Integer port);
}
