package com.avrix.events;

import zombie.iso.weather.WeatherPeriod;

/**
 * Triggered when a weather period stage starts.
 */
public abstract class OnWeatherPeriodStageEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnWeatherPeriodStage";
    }

    /**
     * Called Event Handling Method
     *
     * @param weatherPeriod The weather period.
     */
    public abstract void handleEvent(WeatherPeriod weatherPeriod);
}
