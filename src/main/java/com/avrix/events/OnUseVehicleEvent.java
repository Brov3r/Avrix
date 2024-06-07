package com.avrix.events;

import zombie.characters.IsoPlayer;
import zombie.vehicles.BaseVehicle;

/**
 * Triggered when a character is using a vehicle.
 */
public abstract class OnUseVehicleEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnUseVehicle";
    }

    /**
     * Called Event Handling Method
     *
     * @param player           The player who's driving the vehicle.
     * @param baseVehicle      The vehicle which the player is driving.
     * @param pressedNotTapped Whether the player had been keeping the accelerator pressed or if it was just a tap.
     */
    public abstract void handleEvent(IsoPlayer player, BaseVehicle baseVehicle, Boolean pressedNotTapped);
}
