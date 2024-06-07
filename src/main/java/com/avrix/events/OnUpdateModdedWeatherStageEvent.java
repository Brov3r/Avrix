package com.avrix.events;

import zombie.iso.weather.WeatherPeriod;

/**
 * Triggered when the modded weather sage is being updated.
 */
public abstract class OnUpdateModdedWeatherStageEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnUpdateModdedWeatherStage";
    }

    /**
     * Called Event Handling Method
     *
     * @param weatherPeriod The current weather period.
     * @param weatherStage  The current stage of the weather.
     * @param strength      The strength of the air front.
     */
    public abstract void handleEvent(WeatherPeriod weatherPeriod, WeatherPeriod.WeatherStage weatherStage, Float strength);
}
