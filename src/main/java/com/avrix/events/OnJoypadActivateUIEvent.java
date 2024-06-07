package com.avrix.events;

/**
 * Triggered when joypad is activated from main screen.
 */
public abstract class OnJoypadActivateUIEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnJoypadActivateUI";
    }

    /**
     * Called Event Handling Method
     *
     * @param joypadId The identifier of the joypad.
     */
    public abstract void handleEvent(Integer joypadId);
}
