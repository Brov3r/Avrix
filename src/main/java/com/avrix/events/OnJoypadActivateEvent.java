package com.avrix.events;

/**
 * Triggered when a joypad is activated in-game.
 */
public abstract class OnJoypadActivateEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnJoypadActivate";
    }

    /**
     * Called Event Handling Method
     *
     * @param controllerId The identifier of the joypad which has been activated.
     */
    public abstract void handleEvent(Integer controllerId);
}
