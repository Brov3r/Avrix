package com.avrix.events;

import zombie.characters.IsoGameCharacter;
import zombie.characters.skills.PerkFactory;

/**
 * Triggered when a player gains XP.
 */
public abstract class OnAddXPEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "AddXP";
    }

    /**
     * Called Event Handling Method
     *
     * @param character The character who's gaining XP.
     * @param perk      The perk that is being leveled up.
     * @param level     The perk level gained.
     */
    public abstract void handleEvent(IsoGameCharacter character, PerkFactory.Perk perk, Float level);
}
