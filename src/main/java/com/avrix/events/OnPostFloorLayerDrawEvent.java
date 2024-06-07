package com.avrix.events;

/**
 * Triggered after a floor layer is rendered.
 */
public abstract class OnPostFloorLayerDrawEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnPostFloorLayerDraw";
    }

    /**
     * Called Event Handling Method
     *
     * @param z The z coordinate of the layer which was rendered.
     */
    public abstract void handleEvent(Integer z);
}
