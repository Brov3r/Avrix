package com.avrix.events;

/**
 * Triggered when a keyboard key is being held down.
 */
public abstract class OnKeyKeepPressedEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnKeyKeepPressed";
    }

    /**
     * Called Event Handling Method
     *
     * @param key The Keyboard key that has been kept pressed.
     */
    public abstract void handleEvent(Integer key);
}
