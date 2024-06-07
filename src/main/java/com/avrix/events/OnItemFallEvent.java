package com.avrix.events;

import zombie.inventory.InventoryItem;

/**
 * Triggered when an item is being dropped on the ground.
 */
public abstract class OnItemFallEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "onItemFall";
    }

    /**
     * Called Event Handling Method
     *
     * @param item The inventory item being dropped on the ground.
     */
    public abstract void handleEvent(InventoryItem item);
}
