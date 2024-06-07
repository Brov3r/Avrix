package com.avrix.events;

import zombie.characters.IsoGameCharacter;

/**
 * Triggered when a character dies.
 */
public abstract class OnCharacterDeathEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnCharacterDeath";
    }

    /**
     * Called Event Handling Method
     *
     * @param character The character who's about to die.
     */
    public abstract void handleEvent(IsoGameCharacter character);
}
