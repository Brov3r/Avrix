package com.avrix.events;

import zombie.characters.IsoGameCharacter;
import zombie.characters.skills.PerkFactory;

/**
 * Triggered when a perk is being leveled up.
 */
public abstract class OnLevelPerkEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "LevelPerk";
    }

    /**
     * Called Event Handling Method
     *
     * @param character The character whose perk is being leveled up or down.
     * @param perk      The perk being leveled up or down.
     * @param level     Perk level.
     * @param levelUp   Whether the perk is being leveled up.
     */
    public abstract void handleEvent(IsoGameCharacter character, PerkFactory.Perk perk, Integer level, Boolean levelUp);
}
