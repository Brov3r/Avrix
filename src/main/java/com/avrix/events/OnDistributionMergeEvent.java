package com.avrix.events;

/**
 * Triggered before merging the world distribution.
 */
public abstract class OnDistributionMergeEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnDistributionMerge";
    }

    /**
     * Called Event Handling Method
     */
    public abstract void handleEvent();
}
