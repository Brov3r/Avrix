package com.avrix.events;

/**
 * Triggered when a joypad was connected, just after it's been activated.
 */
public abstract class OnJoypadReactivateEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnJoypadReactivate";
    }

    /**
     * Called Event Handling Method
     *
     * @param joypadId The identifier of the joypad.
     */
    public abstract void handleEvent(Double joypadId);
}
