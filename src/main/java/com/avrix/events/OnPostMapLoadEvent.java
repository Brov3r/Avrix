package com.avrix.events;

import zombie.iso.IsoCell;

/**
 * Triggered after a cell is loaded.
 */
public abstract class OnPostMapLoadEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnPostMapLoad";
    }

    /**
     * Called Event Handling Method
     *
     * @param cell   The cell which was loaded.
     * @param worldX The world x coordinate of the cell which was loaded.
     * @param worldY The world y coordinate of the cell which was loaded.
     */
    public abstract void handleEvent(IsoCell cell, Integer worldX, Integer worldY);
}
