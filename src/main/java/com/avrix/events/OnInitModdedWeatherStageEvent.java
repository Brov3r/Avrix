package com.avrix.events;

import zombie.iso.weather.WeatherPeriod;

/**
 * Triggered when the modded weather state is being initialized.
 */
public abstract class OnInitModdedWeatherStageEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnInitModdedWeatherStage";
    }

    /**
     * Called Event Handling Method
     *
     * @param weatherPeriod    The weather period of this weather stage.
     * @param weatherStage     The weather stage to be initialized.
     * @param airFrontStrength TODO
     */
    public abstract void handleEvent(WeatherPeriod weatherPeriod, WeatherPeriod.WeatherStage weatherStage, Float airFrontStrength);
}
