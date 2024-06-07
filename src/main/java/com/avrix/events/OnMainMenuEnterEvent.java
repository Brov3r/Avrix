package com.avrix.events;

/**
 * Triggered when main menu is displayed to users. This can occur either when they launch the game, or when they quit a running game.
 */
public abstract class OnMainMenuEnterEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnMainMenuEnter";
    }

    /**
     * Called Event Handling Method
     */
    public abstract void handleEvent();
}
