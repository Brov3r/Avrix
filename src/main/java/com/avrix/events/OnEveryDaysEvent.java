package com.avrix.events;

/**
 * Triggered every day at midnight (in-game).
 */
public abstract class OnEveryDaysEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "EveryDays";
    }

    /**
     * Called Event Handling Method
     */
    public abstract void handleEvent();
}
