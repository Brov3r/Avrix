package com.avrix.api.events;

import zombie.vehicles.BaseVehicle;

/**
 * TODO: Description
 */
public abstract class OnSpawnVehicleStartEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnSpawnVehicleStart";
    }

    /**
     * Called Event Handling Method
     *
     * @param vehicle todo
     */
    public abstract void handle(BaseVehicle vehicle);
}
