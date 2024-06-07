package com.avrix.events;

import zombie.characters.IsoPlayer;
import zombie.inventory.InventoryItem;

/**
 * Triggered when a player adds an item to a trade.
 */
public abstract class OnTradingUIAddItemEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "TradingUIAddItem";
    }

    /**
     * Called Event Handling Method
     *
     * @param player        The player who's adding an item to the trade.
     * @param inventoryItem The item which the player is adding to the trade.
     */
    public abstract void handleEvent(IsoPlayer player, InventoryItem inventoryItem);
}
