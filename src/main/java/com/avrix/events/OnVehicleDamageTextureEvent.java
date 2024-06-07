package com.avrix.events;

import zombie.characters.IsoGameCharacter;

/**
 * Triggered when the texture of a vehicle part is changed after being damaged.
 */
public abstract class OnVehicleDamageTextureEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnVehicleDamageTexture";
    }

    /**
     * Called Event Handling Method
     *
     * @param character The player who's driving the vehicle.
     */
    public abstract void handleEvent(IsoGameCharacter character);
}
