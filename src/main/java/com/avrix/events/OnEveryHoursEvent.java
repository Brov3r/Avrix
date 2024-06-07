package com.avrix.events;

/**
 * Triggered every hour (in-game).
 */
public abstract class OnEveryHoursEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "EveryHours";
    }

    /**
     * Called Event Handling Method
     */
    public abstract void handleEvent();
}
