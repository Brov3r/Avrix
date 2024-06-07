package com.avrix.events;

import zombie.inventory.ItemContainer;

/**
 * Triggered after a container has been filled.
 */
public abstract class OnFillContainerEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnFillContainer";
    }

    /**
     * Called Event Handling Method
     *
     * @param roomName      The room name in which the container is installed.
     * @param containerType The type of the container that is being filled.
     * @param itemContainer The container that is being filled.
     */
    public abstract void handleEvent(String roomName, String containerType, ItemContainer itemContainer);
}
