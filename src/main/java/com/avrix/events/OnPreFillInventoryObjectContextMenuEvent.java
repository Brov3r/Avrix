package com.avrix.events;

import se.krka.kahlua.vm.KahluaTable;

/**
 * Triggered before context menus get filled with options.
 */
public abstract class OnPreFillInventoryObjectContextMenuEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnPreFillInventoryObjectContextMenu";
    }

    /**
     * Called Event Handling Method
     *
     * @param playerID The player ID for which the context menu is being filled.
     * @param context  The context menu to be filled.
     * @param items    The items available in the player inventory.
     */
    public abstract void handleEvent(Integer playerID, KahluaTable context, KahluaTable items);
}
