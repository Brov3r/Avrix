package com.avrix.events;

/**
 * Triggered when the game client receives the response after intiating a ping to a server.
 */
public abstract class OnServerPingedEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "ServerPinged";
    }

    /**
     * Called Event Handling Method
     *
     * @param ipAddress The IP address of the user who pinged the server.
     * @param user      The name of the user who pinged the server.
     */
    public abstract void handleEvent(String ipAddress, String user);
}
