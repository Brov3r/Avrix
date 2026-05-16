package com.avrix.api.events;

import zombie.characters.IsoPlayer;
import zombie.characters.animals.AnimalTracks;

import java.util.ArrayList;

/**
 * TODO: Description
 */
public abstract class OnAnimalTracksEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnAnimalTracks";
    }

    /**
     * Called Event Handling Method
     *
     * @param player       todo
     * @param animalTracks todo
     */
    public abstract void handle(IsoPlayer player, ArrayList<AnimalTracks> animalTracks);
}
