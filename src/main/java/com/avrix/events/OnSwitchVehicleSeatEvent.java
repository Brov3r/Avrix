package com.avrix.events;

import zombie.characters.IsoPlayer;

/**
 * Triggered when a character is switching seat in a vehicle.
 */
public abstract class OnSwitchVehicleSeatEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnSwitchVehicleSeat";
    }

    /**
     * Called Event Handling Method
     *
     * @param player The player who's switching seat in the vehicle.
     */
    public abstract void handleEvent(IsoPlayer player);
}
