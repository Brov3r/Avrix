package com.avrix.events;

import zombie.characters.IsoPlayer;

/**
 * Triggered when a player updates the item state of a trade.
 */
public abstract class OnTradingUIUpdateStateEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "TradingUIUpdateState";
    }

    /**
     * Called Event Handling Method
     *
     * @param player    The player who's updating an item.
     * @param itemIndex The index of the item that the player is updating.
     */
    public abstract void handleEvent(IsoPlayer player, Integer itemIndex);
}
