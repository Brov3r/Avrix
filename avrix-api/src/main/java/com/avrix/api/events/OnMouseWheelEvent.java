package com.avrix.api.events;

/**
 * TODO: Description
 */
public abstract class OnMouseWheelEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnMouseWheel";
    }

    /**
     * Called Event Handling Method
     *
     * @param wheel todo
     */
    public abstract void handle(Double wheel);
}
