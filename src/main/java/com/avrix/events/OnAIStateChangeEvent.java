package com.avrix.events;

import zombie.ai.State;
import zombie.characters.IsoGameCharacter;

/**
 * Triggered before an AI state is being changed.
 */
public abstract class OnAIStateChangeEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnAIStateChange";
    }

    /**
     * Called Event Handling Method
     *
     * @param character The character whose AI state is being changed.
     * @param newState  The new AI state.
     * @param oldState  The old AI state.
     */
    public abstract void handleEvent(IsoGameCharacter character, State newState, State oldState);
}
