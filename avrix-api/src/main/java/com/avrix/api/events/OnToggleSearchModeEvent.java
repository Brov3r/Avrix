package com.avrix.api.events;

import zombie.characters.IsoPlayer;

/**
 * Triggered when search mode is being toggled.
 */
public abstract class OnToggleSearchModeEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "onToggleSearchMode";
    }

    /**
     * Called Event Handling Method
     *
     * @param player       The player who's toggling search mode.
     * @param isSearchMode Whether search mode is being enabled or disabled.
     */
    public abstract void handle(IsoPlayer player, Boolean isSearchMode);
}
