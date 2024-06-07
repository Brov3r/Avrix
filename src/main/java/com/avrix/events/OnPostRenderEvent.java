package com.avrix.events;

/**
 * Triggered every time after a frame is rendered in-game.
 */
public abstract class OnPostRenderEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnPostRender";
    }

    /**
     * Called Event Handling Method
     */
    public abstract void handleEvent();
}
