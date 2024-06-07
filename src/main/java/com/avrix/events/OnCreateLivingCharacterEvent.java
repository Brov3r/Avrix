package com.avrix.events;

import zombie.characters.IsoPlayer;
import zombie.characters.IsoSurvivor;
import zombie.characters.SurvivorDesc;

/**
 * Triggered when either a player or survivor is being created.
 */
public abstract class OnCreateLivingCharacterEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnCreateLivingCharacter";
    }

    /**
     * Called Event Handling Method
     *
     * @param playerOrSurvivor The player or survivor who's being created.
     * @param survivorDesc     The survivor description of the player or survivor who's being created.
     */
    public abstract void handleEvent(IsoPlayer playerOrSurvivor, SurvivorDesc survivorDesc);

    /**
     * Called Event Handling Method
     *
     * @param playerOrSurvivor The player or survivor who's being created.
     * @param survivorDesc     The survivor description of the player or survivor who's being created.
     */
    public abstract void handleEvent(IsoSurvivor playerOrSurvivor, SurvivorDesc survivorDesc);
}
