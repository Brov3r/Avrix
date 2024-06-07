package com.avrix.events;

import zombie.characters.IsoGameCharacter;
import zombie.inventory.types.HandWeapon;

/**
 * Triggered when a character hits a tree with a hand weapon.
 */
public abstract class OnWeaponHitTreeEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnWeaponHitTree";
    }

    /**
     * Called Event Handling Method
     *
     * @param character  The character whose weapon hit a tree.
     * @param handWeapon The hand weapon used to hit the tree.
     */
    public abstract void handleEvent(IsoGameCharacter character, HandWeapon handWeapon);
}
