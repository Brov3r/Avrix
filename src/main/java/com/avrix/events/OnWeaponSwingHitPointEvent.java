package com.avrix.events;

import zombie.characters.IsoGameCharacter;
import zombie.inventory.types.HandWeapon;

/**
 * Triggered when a hand weapon has reached the apex of its swing.
 */
public abstract class OnWeaponSwingHitPointEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnWeaponSwingHitPoint";
    }

    /**
     * Called Event Handling Method
     *
     * @param character  The character who's wielding the weapon.
     * @param handWeapon The hand weapon that is being wielded.
     */
    public abstract void handleEvent(IsoGameCharacter character, HandWeapon handWeapon);
}
