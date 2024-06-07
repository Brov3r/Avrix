package com.avrix.events;

import zombie.characters.IsoPlayer;

/**
 * Triggered when search mode is being disabled.
 */
public abstract class OnDisableSearchModeEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "onDisableSearchMode";
    }

    /**
     * Called Event Handling Method
     *
     * @param player       The player who's disabling search mode.
     * @param isSearchMode Whether search mode is being enabled or disabled.
     */
    public abstract void handleEvent(IsoPlayer player, Boolean isSearchMode);
}
