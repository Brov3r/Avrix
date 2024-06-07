package com.avrix.events;

import zombie.iso.areas.IsoRoom;

/**
 * Triggered for each room about to get spawned, the first time a character gets close enough to the building where the room is located.
 */
public abstract class OnSeeNewRoomEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnSeeNewRoom";
    }

    /**
     * Called Event Handling Method
     *
     * @param room The room about to get spawned.
     */
    public abstract void handleEvent(IsoRoom room);
}
