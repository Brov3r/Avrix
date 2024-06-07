package com.avrix.events;

/**
 * Triggered every ten minutes (in-game).
 */
public abstract class OnEveryTenMinutesEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "EveryTenMinutes";
    }

    /**
     * Called Event Handling Method
     */
    public abstract void handleEvent();
}
