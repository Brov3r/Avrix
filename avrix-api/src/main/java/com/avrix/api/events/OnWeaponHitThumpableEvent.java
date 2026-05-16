package com.avrix.api.events;

import zombie.characters.IsoGameCharacter;
import zombie.inventory.types.HandWeapon;
import zombie.iso.objects.*;

/**
 * Fires when an IsoThumpable is hit by an attack. This event is triggered before any damage is applied to thumpable object.
 */
public abstract class OnWeaponHitThumpableEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnWeaponHitThumpable";
    }

    /**
     * Called Event Handling Method
     *
     * @param attacker The character attacking the object.
     * @param weapon   The weapon the object was attacked with.
     * @param object   The object that was attacked. This object can also be an instance of IsoBarricade (JavaDoc), IsoCompost (JavaDoc), IsoDoor (JavaDoc), IsoWindow (JavaDoc). Those are a bit different from IsoThumpable in their health manipulation methods, check Java docs!
     */
    public abstract void handle(IsoGameCharacter attacker, HandWeapon weapon, IsoThumpable object);

    /**
     * Called Event Handling Method
     *
     * @param attacker The character attacking the object.
     * @param weapon   The weapon the object was attacked with.
     * @param object   The object that was attacked. This object can also be an instance of IsoBarricade (JavaDoc), IsoCompost (JavaDoc), IsoDoor (JavaDoc), IsoWindow (JavaDoc). Those are a bit different from IsoThumpable in their health manipulation methods, check Java docs!
     */
    public abstract void handle(IsoGameCharacter attacker, HandWeapon weapon, IsoBarricade object);

    /**
     * Called Event Handling Method
     *
     * @param attacker The character attacking the object.
     * @param weapon   The weapon the object was attacked with.
     * @param object   The object that was attacked. This object can also be an instance of IsoBarricade (JavaDoc), IsoCompost (JavaDoc), IsoDoor (JavaDoc), IsoWindow (JavaDoc). Those are a bit different from IsoThumpable in their health manipulation methods, check Java docs!
     */
    public abstract void handle(IsoGameCharacter attacker, HandWeapon weapon, IsoCompost object);

    /**
     * Called Event Handling Method
     *
     * @param attacker The character attacking the object.
     * @param weapon   The weapon the object was attacked with.
     * @param object   The object that was attacked. This object can also be an instance of IsoBarricade (JavaDoc), IsoCompost (JavaDoc), IsoDoor (JavaDoc), IsoWindow (JavaDoc). Those are a bit different from IsoThumpable in their health manipulation methods, check Java docs!
     */
    public abstract void handle(IsoGameCharacter attacker, HandWeapon weapon, IsoDoor object);

    /**
     * Called Event Handling Method
     *
     * @param attacker The character attacking the object.
     * @param weapon   The weapon the object was attacked with.
     * @param object   The object that was attacked. This object can also be an instance of IsoBarricade (JavaDoc), IsoCompost (JavaDoc), IsoDoor (JavaDoc), IsoWindow (JavaDoc). Those are a bit different from IsoThumpable in their health manipulation methods, check Java docs!
     */
    public abstract void handle(IsoGameCharacter attacker, HandWeapon weapon, IsoWindow object);
}
