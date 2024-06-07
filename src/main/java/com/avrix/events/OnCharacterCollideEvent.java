package com.avrix.events;

import zombie.characters.IsoGameCharacter;

/**
 * Triggered when two characters collide together.
 */
public abstract class OnCharacterCollideEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnCharacterCollide";
    }

    /**
     * Called Event Handling Method
     *
     * @param player    The character who's colliding with another character.
     * @param character The character who's being collided with.
     */
    public abstract void handleEvent(IsoGameCharacter player, IsoGameCharacter character);
}
