package com.avrix.api.events;

/**
 * TODO: Description
 */
public abstract class OnSourceWindowFileReloadEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnSourceWindowFileReload";
    }

    /**
     * Called Event Handling Method
     */
    public abstract void handle();
}
