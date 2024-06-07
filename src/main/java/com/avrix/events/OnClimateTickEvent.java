package com.avrix.events;

import zombie.iso.weather.ClimateManager;

/**
 * Triggered for every climate tick.
 */
public abstract class OnClimateTickEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnClimateTick";
    }

    /**
     * Called Event Handling Method
     *
     * @param climateManager The climate manager.
     */
    public abstract void handleEvent(ClimateManager climateManager);
}
