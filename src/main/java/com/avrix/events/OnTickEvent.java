package com.avrix.events;

/**
 * Triggered every tick, try to not use this one, use EveryTenMinutes instead because it can create a lot of frame loss/garbage collection.
 */
public abstract class OnTickEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnTick";
    }

    /**
     * Called Event Handling Method
     *
     * @param numberTicks The number of ticks.
     */
    public abstract void handleEvent(Double numberTicks);
}
