package com.avrix.api.events;

import zombie.characters.IsoPlayer;

/**
 * TODO: Description
 */
public abstract class OnContextKeyEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnContextKey";
    }

    /**
     * Called Event Handling Method
     *
     * @param player             todo
     * @param timePressedContext todo
     */
    public abstract void handle(IsoPlayer player, Double timePressedContext);
}
