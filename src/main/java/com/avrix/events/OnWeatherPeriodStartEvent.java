package com.avrix.events;

import zombie.iso.weather.WeatherPeriod;

/**
 * Triggered when a weather period starts.
 */
public abstract class OnWeatherPeriodStartEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnWeatherPeriodStart";
    }

    /**
     * Called Event Handling Method
     *
     * @param weatherPeriod The weather period.
     */
    public abstract void handleEvent(WeatherPeriod weatherPeriod);
}
