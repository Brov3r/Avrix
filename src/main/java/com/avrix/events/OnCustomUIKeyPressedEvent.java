package com.avrix.events;

/**
 * Triggered when a custom UI key has been pressed.
 */
public abstract class OnCustomUIKeyPressedEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnCustomUIKeyPressed";
    }

    /**
     * Called Event Handling Method
     *
     * @param key The Keyboard key that has been pressed.
     */
    public abstract void handleEvent(Integer key);
}
