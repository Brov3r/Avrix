package com.avrix.events;

/**
 * Client window full initialization events
 */
public abstract class OnGameWindowInitializedEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "onGameWindowInitialized";
    }

    ;

    /**
     * Called Event Handling Method
     */
    public abstract void handleEvent();
}
