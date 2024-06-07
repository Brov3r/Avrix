package com.avrix.events;

/**
 * Triggered when a joypad was disconnected, just before being deactivated.
 */
public abstract class OnJoypadBeforeDeactivateEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnJoypadBeforeDeactivate";
    }

    /**
     * Called Event Handling Method
     *
     * @param joypadId The identifier of the joypad.
     */
    public abstract void handleEvent(Double joypadId);
}
