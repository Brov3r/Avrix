package com.avrix.events;

/**
 * Triggered when a gamepad has been disconnected.
 */
public abstract class OnGamepadDisconnectEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnGamepadDisconnect";
    }

    /**
     * Called Event Handling Method
     *
     * @param controllerID The identifier of the gamepad which has been disconnected.
     */
    public abstract void handleEvent(Integer controllerID);
}
