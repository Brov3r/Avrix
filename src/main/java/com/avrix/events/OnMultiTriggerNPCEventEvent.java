package com.avrix.events;

/**
 * No description available.
 */
public abstract class OnMultiTriggerNPCEventEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnMultiTriggerNPCEvent";
    }

    /**
     * Called Event Handling Method
     */
    public abstract void handleEvent();
}
