package com.avrix.events;

/**
 * Triggered when a joypad was disconnected, just after it's been deactivated.
 */
public abstract class OnJoypadDeactivateEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnJoypadDeactivate";
    }

    /**
     * Called Event Handling Method
     *
     * @param joypadId The identifier of the joypad.
     */
    public abstract void handleEvent(Double joypadId);
}
