package com.avrix.api.events;

import se.krka.kahlua.vm.KahluaTable;
import zombie.characters.IsoPlayer;
import zombie.inventory.InventoryItem;
import zombie.network.fields.ContainerID;

/**
 * TODO: Description
 */
public abstract class OnProcessTransactionEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnProcessTransaction";
    }

    /**
     * Called Event Handling Method
     *
     * @param type          todo
     *                      "scrapMoveable"
     *                      "pickUpMoveable"
     *                      "rotateMoveable"
     *                      "placeMoveable"
     *                      "dropOnFloor"
     * @param player        todo
     * @param item          todo
     * @param sourceId      todo
     * @param destinationId todo
     * @param table         When type is "dropOnFloor", has field IsoGridSquare "square". When type is "rotateMoveable" or "placeMoveable", has field string "direction"
     */
    public abstract void handle(String type, IsoPlayer player, InventoryItem item, ContainerID sourceId, ContainerID destinationId, KahluaTable table);
}
