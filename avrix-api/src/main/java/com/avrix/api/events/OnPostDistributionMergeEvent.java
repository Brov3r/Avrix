package com.avrix.api.events;

/**
 * Triggered after the distribution merge.
 */
public abstract class OnPostDistributionMergeEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnPostDistributionMerge";
    }

    /**
     * Called Event Handling Method
     */
    public abstract void handle();
}
