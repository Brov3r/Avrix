package com.avrix.api.events;

import zombie.characters.IsoGameCharacter;
import zombie.inventory.types.HandWeapon;

/**
 * TODO: Description
 */
public abstract class OnGrappleGrabCollisionCheckEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "GrappleGrabCollisionCheck";
    }

    /**
     * Called Event Handling Method
     *
     * @param character todo
     * @param weapon    todo
     */
    public abstract void handle(IsoGameCharacter character, HandWeapon weapon);
}
