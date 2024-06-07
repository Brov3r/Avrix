package com.avrix.events;

import zombie.characters.IsoPlayer;
import zombie.vehicles.BaseVehicle;

/**
 * Triggered when a character is using a vehicle horn.
 */
public abstract class OnVehicleHornEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnVehicleHorn";
    }

    /**
     * Called Event Handling Method
     *
     * @param player      The player who's driving the vehicle.
     * @param baseVehicle The vehicle that the player is driving.
     * @param pressed     Whether the vehicle horn is being pressed.
     */
    public abstract void handleEvent(IsoPlayer player, BaseVehicle baseVehicle, Boolean pressed);
}
