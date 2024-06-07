package com.avrix.events;

/**
 * Same as OnTick, except is only called while on the main menu.
 */
public abstract class OnFETickEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnFETick";
    }

    /**
     * Called Event Handling Method
     *
     * @param numberTicks Always zero.
     */
    public abstract void handleEvent(Double numberTicks);
}
