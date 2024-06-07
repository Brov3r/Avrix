package com.avrix.events;

/**
 * Triggered before the distribution merge.
 */
public abstract class OnPreDistributionMergeEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnPreDistributionMerge";
    }

    /**
     * Called Event Handling Method
     */
    public abstract void handleEvent();
}
