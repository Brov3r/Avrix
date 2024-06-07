package com.avrix.events;

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
    public abstract void handleEvent(Food container);

    /**
     * Called Event Handling Method
     *
     * @param container The container which is being updated.
     */
    public abstract void handleEvent(IsoDeadBody container);

    /**
     * Called Event Handling Method
     *
     * @param container The container which is being updated.
     */
    public abstract void handleEvent(IsoGridSquare container);

    /**
     * Called Event Handling Method
     *
     * @param container The container which is being updated.
     */
    public abstract void handleEvent(IsoWorldInventoryObject container);
}
