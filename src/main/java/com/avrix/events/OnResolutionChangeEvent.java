package com.avrix.events;

/**
 * Triggered when game resolution has changed.
 */
public abstract class OnResolutionChangeEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnResolutionChange";
    }

    /**
     * Called Event Handling Method
     *
     * @param oldWidth  The old width of the screen.
     * @param oldHeight The old height of the screen.
     * @param newWidth  The new width of the screen.
     * @param newHeight The new height of the screen.
     */
    public abstract void handleEvent(Integer oldWidth, Integer oldHeight, Integer newWidth, Integer newHeight);
}
