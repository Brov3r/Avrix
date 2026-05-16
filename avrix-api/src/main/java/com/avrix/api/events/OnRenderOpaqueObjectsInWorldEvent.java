package com.avrix.api.events;

import zombie.iso.IsoGridSquare;

/**
 * TODO: Description
 */
public abstract class OnRenderOpaqueObjectsInWorldEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "RenderOpaqueObjectsInWorld";
    }

    /**
     * Called Event Handling Method
     *
     * @param playerNum todo
     * @param x         todo
     * @param y         todo
     * @param z         todo
     * @param square    todo
     */
    public abstract void handle(Integer playerNum, Integer x, Integer y, Integer z, IsoGridSquare square);
}
