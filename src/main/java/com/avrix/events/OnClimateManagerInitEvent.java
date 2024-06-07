package com.avrix.events;

import zombie.iso.weather.ClimateManager;

/**
 * Triggered after the ClimateManager has been initialized.
 */
public abstract class OnClimateManagerInitEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnClimateManagerInit";
    }

    /**
     * Called Event Handling Method
     *
     * @param climateManager The climate manager which is being initialized.
     */
    public abstract void handleEvent(ClimateManager climateManager);
}
