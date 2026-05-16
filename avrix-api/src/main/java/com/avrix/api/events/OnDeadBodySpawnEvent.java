package com.avrix.api.events;

import zombie.iso.objects.IsoDeadBody;

/**
 * TODO: Description
 */
public abstract class OnDeadBodySpawnEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnDeadBodySpawn";
    }

    /**
     * Called Event Handling Method
     *
     * @param body todo
     */
    public abstract void handle(IsoDeadBody body);
}
