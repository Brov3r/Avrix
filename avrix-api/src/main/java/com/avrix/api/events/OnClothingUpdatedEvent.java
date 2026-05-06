package com.avrix.api.events;

import zombie.characters.IsoPlayer;

/**
 * Triggered when a character's clothing items are updated.
 */
public abstract class OnClothingUpdatedEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnClothingUpdated";
    }

    /**
     * Called Event Handling Method
     *
     * @param playerOrCharacter The character whose clothing has been updated.
     */
    public abstract void handle(IsoPlayer playerOrCharacter);
}
