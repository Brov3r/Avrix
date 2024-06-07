package com.avrix.events;

import zombie.characters.IsoGameCharacter;

/**
 * Triggered when a character is exiting a vehicle.
 */
public abstract class OnExitVehicleEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnExitVehicle";
    }

    /**
     * Called Event Handling Method
     *
     * @param character The character who's exiting the vehicle.
     */
    public abstract void handleEvent(IsoGameCharacter character);
}
