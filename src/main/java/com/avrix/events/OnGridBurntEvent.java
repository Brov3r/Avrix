package com.avrix.events;

import zombie.iso.IsoGridSquare;

/**
 * Triggered when a grid square is burning.
 */
public abstract class OnGridBurntEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnGridBurnt";
    }

    /**
     * Called Event Handling Method
     *
     * @param square The grid square that is burning.
     */
    public abstract void handleEvent(IsoGridSquare square);
}
