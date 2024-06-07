package com.avrix.events;

/**
 * No description available.
 */
public abstract class OnTriggerNPCEventEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnTriggerNPCEvent";
    }

    /**
     * Called Event Handling Method
     */
    public abstract void handleEvent();
}
