package com.avrix.events;

import zombie.iso.weather.ClimateManager;

/**
 * Triggered for every climate tick but only when debug mode is enabled.
 */
public abstract class OnClimateTickDebugEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnClimateTickDebug";
    }

    /**
     * Called Event Handling Method
     *
     * @param climateManager The climate manager.
     */
    public abstract void handleEvent(ClimateManager climateManager);
}
