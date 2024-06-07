package com.avrix.events;

/**
 * Triggered when a keyboard key is initially being pressed.
 */
public abstract class OnKeyStartPressedEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnKeyStartPressed";
    }

    /**
     * Called Event Handling Method
     *
     * @param key The Keyboard key that has been pressed.
     */
    public abstract void handleEvent(Integer key);
}
