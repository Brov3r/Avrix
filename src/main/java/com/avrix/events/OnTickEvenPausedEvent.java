package com.avrix.events;

/**
 * Same as OnTick, but triggered when the game is paused as well.
 */
public abstract class OnTickEvenPausedEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnTickEvenPaused";
    }

    /**
     * Called Event Handling Method
     *
     * @param numberTicks The number of ticks.
     */
    public abstract void handleEvent(Double numberTicks);
}
