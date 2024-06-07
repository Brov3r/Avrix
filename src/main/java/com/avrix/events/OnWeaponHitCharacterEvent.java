package com.avrix.events;

import zombie.characters.IsoGameCharacter;
import zombie.inventory.types.HandWeapon;

/**
 * Triggered when a character has been hit by a weapon.
 */
public abstract class OnWeaponHitCharacterEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnWeaponHitCharacter";
    }

    /**
     * Called Event Handling Method
     *
     * @param wielder    The character whose weapon hit another character.
     * @param character  The character who's been hit by another character.
     * @param handWeapon The hand weapon used to hit the character.
     * @param damage     The damage inflicted to the character who's been hit.
     */
    public abstract void handleEvent(IsoGameCharacter wielder, IsoGameCharacter character, HandWeapon handWeapon, Float damage);
}
