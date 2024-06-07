package com.avrix.events;

/**
 * Triggered when every tick after all elements have been rendered. Called from the render thread.
 */
public abstract class OnPostTickRenderThreadEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "onPostTickRenderThread";
    }

    /**
     * Called Event Handling Method
     */
    public abstract void handleEvent();
}
