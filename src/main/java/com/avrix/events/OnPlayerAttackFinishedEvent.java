package com.avrix.events;

import zombie.characters.IsoGameCharacter;
import zombie.inventory.types.HandWeapon;

/**
 * Triggered when a character is done performing an attack.
 */
public abstract class OnPlayerAttackFinishedEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnPlayerAttackFinished";
    }

    /**
     * Called Event Handling Method
     *
     * @param character  The character who's finished attacking.
     * @param handWeapon The hand weapon used to perform the attack.
     */
    public abstract void handleEvent(IsoGameCharacter character, HandWeapon handWeapon);
}
