package com.avrix.events;

/**
 * Triggered when a gamepad has been connected.
 */
public abstract class OnGamepadConnectEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnGamepadConnect";
    }

    /**
     * Called Event Handling Method
     *
     * @param controllerID The identifier of the gamepad which has been connected.
     */
    public abstract void handleEvent(Integer controllerID);
}
