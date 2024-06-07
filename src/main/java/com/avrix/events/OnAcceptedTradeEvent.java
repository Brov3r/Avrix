package com.avrix.events;

/**
 * Triggered when a trade request has been accepted.
 */
public abstract class OnAcceptedTradeEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "AcceptedTrade";
    }

    /**
     * Called Event Handling Method
     *
     * @param accepted Whether the trade was accepted or not.
     */
    public abstract void handleEvent(Boolean accepted);
}
