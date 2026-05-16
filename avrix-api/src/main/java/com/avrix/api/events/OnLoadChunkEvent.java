package com.avrix.api.events;

import zombie.iso.IsoChunk;

/**
 * TODO: Description
 */
public abstract class OnLoadChunkEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "LoadChunk";
    }

    /**
     * Called Event Handling Method
     *
     * @param chunk todo
     */
    public abstract void handle(IsoChunk chunk);
}
