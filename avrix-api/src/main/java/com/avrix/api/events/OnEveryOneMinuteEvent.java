package com.avrix.api.events;

/**
 * Triggered every minute (in-game).
 */
public abstract class OnEveryOneMinuteEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "EveryOneMinute";
    }

    /**
     * Called Event Handling Method
     */
    public abstract void handle();
}
