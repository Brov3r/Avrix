package com.avrix.api.events;

import se.krka.kahlua.vm.KahluaTable;

/**
 * Fires when the available containers in the inventory UI change.
 */
public abstract class OnRefreshInventoryWindowContainersEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnRefreshInventoryWindowContainers";
    }

    /**
     * Called Event Handling Method
     *
     * @param table  ISInventoryPage table
     * @param reason todo
     */
    public abstract void handle(KahluaTable table, String reason);
}
