package com.avrix.events;

import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.inventory.types.HandWeapon;

/**
 * Triggered when a player is gaining XP for a successful hit.
 */
public abstract class OnWeaponHitXpEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnWeaponHitXp";
    }

    /**
     * Called Event Handling Method
     *
     * @param player      The player who's wielding the weapon.
     * @param handWeapon  The hand weapon used to perform the attack.
     * @param character   The character who's being hit.
     * @param damageSplit The damage split of the hit.
     */
    public abstract void handleEvent(IsoPlayer player, HandWeapon handWeapon, IsoGameCharacter character, Float damageSplit);
}
