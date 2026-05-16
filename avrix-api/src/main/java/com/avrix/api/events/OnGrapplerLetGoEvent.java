package com.avrix.api.events;

import zombie.characters.IsoPlayer;

/**
 * TODO: Description
 */
public abstract class OnGrapplerLetGoEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "GrapplerLetGo";
    }

    /**
     * Called Event Handling Method
     *
     * @param player  todo
     * @param unknown todo
     */
    public abstract void handle(IsoPlayer player, String unknown);
}
