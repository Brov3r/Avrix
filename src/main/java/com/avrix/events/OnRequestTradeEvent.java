package com.avrix.events;

import zombie.characters.IsoPlayer;

/**
 * Triggered when a character is requesting a trade with another character.
 */
public abstract class OnRequestTradeEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "RequestTrade";
    }

    /**
     * Called Event Handling Method
     *
     * @param player The player who's requesting the trade.
     */
    public abstract void handleEvent(IsoPlayer player);
}
