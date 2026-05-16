package com.avrix.api.events;

import zombie.characters.IsoPlayer;

/**
 * TODO: Description
 */
public abstract class OnStopGrappleEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnStopGrapple";
    }

    /**
     * Called Event Handling Method
     *
     * @param player todo
     */
    public abstract void handle(IsoPlayer player);
}
