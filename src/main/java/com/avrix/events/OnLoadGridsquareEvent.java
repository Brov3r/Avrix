package com.avrix.events;

import zombie.iso.IsoGridSquare;

/**
 * Triggered when a square is being loaded.
 */
public abstract class OnLoadGridsquareEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "LoadGridsquare";
    }

    /**
     * Called Event Handling Method
     *
     * @param square The grid square that is being loaded.
     */
    public abstract void handleEvent(IsoGridSquare square);
}
