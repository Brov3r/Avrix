package com.avrix.events;

/**
 * TODO
 */
public abstract class OnUpdateIconEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "onUpdateIcon";
    }

    /**
     * Called Event Handling Method
     */
    public abstract void handleEvent();
}
