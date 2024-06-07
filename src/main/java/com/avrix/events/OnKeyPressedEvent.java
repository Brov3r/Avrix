package com.avrix.events;

/**
 * Triggered when a keyboard key is being pressed.
 */
public abstract class OnKeyPressedEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnKeyPressed";
    }

    /**
     * Called Event Handling Method
     *
     * @param key The Keyboard key that has been released.
     */
    public abstract void handleEvent(Integer key);
}
