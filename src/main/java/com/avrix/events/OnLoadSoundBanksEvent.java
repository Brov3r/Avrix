package com.avrix.events;

/**
 * Triggered when sound banks are loaded for the game.
 */
public abstract class OnLoadSoundBanksEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnLoadSoundBanks";
    }

    /**
     * Called Event Handling Method
     */
    public abstract void handleEvent();
}
