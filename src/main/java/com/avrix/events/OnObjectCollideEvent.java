package com.avrix.events;

import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoZombie;
import zombie.iso.IsoGridSquare;
import zombie.iso.objects.IsoDoor;
import zombie.iso.objects.IsoThumpable;
import zombie.iso.objects.IsoWindow;

/**
 * Triggered when a character collides with an object.
 */
public abstract class OnObjectCollideEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnObjectCollide";
    }

    /**
     * Called Event Handling Method
     *
     * @param character The character who's colliding with another object.
     * @param door      The object that is being collided with.
     */
    public abstract void handleEvent(IsoGameCharacter character, IsoDoor door);

    /**
     * Called Event Handling Method
     *
     * @param character The character who's colliding with another object.
     * @param door      The object that is being collided with.
     */
    public abstract void handleEvent(IsoGameCharacter character, IsoGridSquare door);

    /**
     * Called Event Handling Method
     *
     * @param character The character who's colliding with another object.
     * @param door      The object that is being collided with.
     */
    public abstract void handleEvent(IsoGameCharacter character, IsoThumpable door);

    /**
     * Called Event Handling Method
     *
     * @param character The character who's colliding with another object.
     * @param door      The object that is being collided with.
     */
    public abstract void handleEvent(IsoGameCharacter character, IsoWindow door);

    /**
     * Called Event Handling Method
     *
     * @param character The character who's colliding with another object.
     * @param door      The object that is being collided with.
     */
    public abstract void handleEvent(IsoGameCharacter character, IsoZombie door);
}
