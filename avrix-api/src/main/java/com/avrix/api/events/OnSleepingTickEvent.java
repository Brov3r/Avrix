package com.avrix.api.events;

/**
 * TODO: Description
 */
public abstract class OnSleepingTickEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnSleepingTick";
    }

    /**
     * Called Event Handling Method
     *
     * @param playerNum todo
     * @param hours     todo
     */
    public abstract void handle(Double playerNum, Double hours);
}
