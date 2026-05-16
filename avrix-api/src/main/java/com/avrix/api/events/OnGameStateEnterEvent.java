package com.avrix.api.events;

import zombie.ai.State;

/**
 * Fires upon entering the Terms Of Service game state.
 */
public abstract class OnGameStateEnterEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnGameStateEnter";
    }

    /**
     * Called Event Handling Method
     *
     * @param state todo
     */
    public abstract void handle(State state);
}
