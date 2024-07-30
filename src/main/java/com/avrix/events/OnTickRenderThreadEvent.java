package com.avrix.events;

/**
 * Triggered when every tick after elements have been rendered. Called from the render thread.
 */
public abstract class OnTickRenderThreadEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "onTickRenderThread";
    }

    /**
     * Called Event Handling Method
     */
    public abstract void handleEvent();
}
