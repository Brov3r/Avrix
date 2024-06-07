package com.avrix.events;

import zombie.characters.IsoGameCharacter;

/**
 * Triggered when a player successfully enters a vehicle.
 */
public abstract class OnEnterVehicleEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnEnterVehicle";
    }

    /**
     * Called Event Handling Method
     *
     * @param character The character who's entering the vehicle.
     */
    public abstract void handleEvent(IsoGameCharacter character);
}
