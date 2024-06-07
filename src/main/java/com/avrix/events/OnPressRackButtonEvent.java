package com.avrix.events;

import zombie.characters.IsoPlayer;
import zombie.inventory.types.HandWeapon;

/**
 * Triggered when a player hits the button to rack a firearm.
 */
public abstract class OnPressRackButtonEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnPressRackButton";
    }

    /**
     * Called Event Handling Method
     *
     * @param player  The player who's racking the firearm.
     * @param firearm The firearm which is being racked.
     */
    public abstract void handleEvent(IsoPlayer player, HandWeapon firearm);
}
