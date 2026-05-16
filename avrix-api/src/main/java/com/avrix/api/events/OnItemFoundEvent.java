package com.avrix.api.events;

import zombie.characters.IsoPlayer;

/**
 * TODO: Description
 */
public abstract class OnItemFoundEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnItemFound";
    }

    /**
     * Called Event Handling Method
     *
     * @param player   todo
     * @param itemType todo
     * @param amount   todo
     */
    public abstract void handle(IsoPlayer player, String itemType, Float amount);
}
