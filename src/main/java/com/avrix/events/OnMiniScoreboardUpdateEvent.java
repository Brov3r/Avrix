package com.avrix.events;

/**
 * Triggered when the mini scoreboard is being updated.
 */
public abstract class OnMiniScoreboardUpdateEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnMiniScoreboardUpdate";
    }

    /**
     * Called Event Handling Method
     */
    public abstract void handleEvent();
}
