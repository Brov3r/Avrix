package com.avrix.events;

/**
 * Triggered when a joypad was connected, just before being activated.
 */
public abstract class OnJoypadBeforeReactivateEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnJoypadBeforeReactivate";
    }

    /**
     * Called Event Handling Method
     *
     * @param joypadId The identifier of the joypad.
     */
    public abstract void handleEvent(Double joypadId);
}
