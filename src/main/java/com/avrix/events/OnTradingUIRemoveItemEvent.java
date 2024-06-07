package com.avrix.events;

import zombie.characters.IsoPlayer;

/**
 * Triggered when a player removes an item from a trade.
 */
public abstract class OnTradingUIRemoveItemEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "TradingUIRemoveItem";
    }

    /**
     * Called Event Handling Method
     *
     * @param player    The player who's removing an item from the trade.
     * @param itemIndex The index of the item that the player is removing from the trade.
     */
    public abstract void handleEvent(IsoPlayer player, Integer itemIndex);
}
