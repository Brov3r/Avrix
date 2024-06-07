package com.avrix.events;

import se.krka.kahlua.vm.KahluaTable;

/**
 * Triggered when inventory object context menus are being filled.
 */
public abstract class OnFillInventoryObjectContextMenuEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnFillInventoryObjectContextMenu";
    }

    /**
     * Called Event Handling Method
     *
     * @param playerIndex The index of the player for which the context menu is being filled.
     * @param table       The context menu to be filled.
     * @param items       The items available in the player inventory.
     */
    public abstract void handleEvent(Integer playerIndex, KahluaTable table, KahluaTable items);
}
