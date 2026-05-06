package com.avrix.api.events;

import zombie.inventory.types.Food;
import zombie.iso.IsoGridSquare;
import zombie.iso.objects.IsoDeadBody;
import zombie.iso.objects.IsoWorldInventoryObject;

/**
 * Triggered when a container is being updated.
 */
public abstract class OnContainerUpdateEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnContainerUpdate";
    }

    /**
     * Called Event Handling Method
     *
     * @param container The container which is being updated.
     */
    public abstract void handle(Food container);

    /**
     * Called Event Handling Method
     *
     * @param container The container which is being updated.
     */
    public abstract void handle(IsoDeadBody container);

    /**
     * Called Event Handling Method
     *
     * @param container The container which is being updated.
     */
    public abstract void handle(IsoGridSquare container);

    /**
     * Called Event Handling Method
     *
     * @param container The container which is being updated.
     */
    public abstract void handle(IsoWorldInventoryObject container);
}
