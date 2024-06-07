package com.avrix.events;

import zombie.network.Server;

/**
 * TODO
 */
public abstract class OnSteamServerResponded2Event extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnSteamServerResponded2";
    }

    /**
     * Called Event Handling Method
     *
     * @param host   TODO
     * @param port   TODO
     * @param server TODO
     */
    public abstract void handleEvent(String host, Integer port, Server server);
}
