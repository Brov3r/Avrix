package com.avrix.events;

import zombie.characters.IsoGameCharacter;
import zombie.inventory.InventoryItem;

/**
 * Triggered when a character equips an item in its primary slot.
 */
public abstract class OnEquipPrimaryEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnEquipPrimary";
    }

    /**
     * Called Event Handling Method
     *
     * @param character     The character who's equipping the item.
     * @param inventoryItem The item that is being equipped in the primary slot.
     */
    public abstract void handleEvent(IsoGameCharacter character, InventoryItem inventoryItem);
}
