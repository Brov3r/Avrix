package com.avrix.events;

/**
 * Triggered every time the screen is being rendered, whether in-game or on main screen.
 */
public abstract class OnJoypadRenderUIEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnJoypadRenderUI";
    }

    /**
     * Called Event Handling Method
     */
    public abstract void handleEvent();
}
