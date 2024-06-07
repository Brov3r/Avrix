package com.avrix.events;

import zombie.iso.IsoChunk;

/**
 * Triggered when a building tile is being set.
 */
public abstract class OnDoTileBuilding3Event extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnDoTileBuilding3";
    }

    /**
     * Called Event Handling Method
     *
     * @param chunk  The chunk in which the tile is being set.
     * @param render Whether the tile should be rendered or not.
     * @param x      The x coordinate of the tile being set.
     * @param y      The y coordinate of the tile being set.
     * @param z      The z coordinate of the tile being set.
     */
    public abstract void handleEvent(IsoChunk chunk, Boolean render, Integer x, Integer y, Integer z);
}
