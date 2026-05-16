package com.avrix.api.events;

import zombie.characters.IsoGameCharacter;

/**
 * Fires every time a local player takes damage. Bleeding bodyparts fire the event once per frame each. It also fires when zombies are hit by weapons: this is the only case in which the event fires on the server.
 */
public abstract class OnPlayerGetDamageEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnPlayerGetDamage";
    }

    /**
     * Called Event Handling Method
     *
     * @param character  The character who took damage.
     * @param damageType The type of damage the character took.
     *                   "POISON"
     *                   "HUNGRY"
     *                   "SICK"
     *                   "BLEEDING"
     *                   "THIRST"
     *                   "HEAVYLOAD"
     *                   "INFECTION"
     *                   "LOWWEIGHT"
     *                   "FALLDOWN"
     *                   "WEAPONHIT"
     *                   "CARHITDAMAGE"
     *                   "CARCRASHDAMAGE"
     * @param damage     The damage that was taken.
     */
    public abstract void handle(IsoGameCharacter character, String damageType, Float damage);
}
