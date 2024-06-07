package com.avrix.events;

import zombie.iso.IsoGridSquare;

/**
 * Triggered when a grid square is being reused.
 */
public abstract class OnReuseGridsquareEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "ReuseGridsquare";
    }

    /**
     * Called Event Handling Method
     *
     * @param square The grid square that is going to be reused.
     */
    public abstract void handleEvent(IsoGridSquare square);
}
