package com.avrix.events;

import zombie.characters.IsoPlayer;
import zombie.inventory.types.HandWeapon;

/**
 * Triggered when a player hits the button to reload a firearm.
 */
public abstract class OnPressReloadButtonEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnPressReloadButton";
    }

    /**
     * Called Event Handling Method
     *
     * @param player  The player who's reloading the firearm.
     * @param firearm The firearm which is being reloaded.
     */
    public abstract void handleEvent(IsoPlayer player, HandWeapon firearm);
}
