package com.avrix.events;

/**
 * Triggered when a custom UI key has been released.
 */
public abstract class OnCustomUIKeyEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnCustomUIKey";
    }

    /**
     * Called Event Handling Method
     *
     * @param key The Keyboard key that has been released.
     */
    public abstract void handleEvent(Integer key);
}
