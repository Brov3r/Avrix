package com.avrix.events;

/**
 * Triggered every time the display is being rendered.
 */
public abstract class OnRenderTickEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnRenderTick";
    }

    /**
     * Called Event Handling Method
     */
    public abstract void handleEvent();
}
