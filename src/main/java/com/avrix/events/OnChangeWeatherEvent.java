package com.avrix.events;

/**
 * Triggered when the weather is changing.
 */
public abstract class OnChangeWeatherEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnChangeWeather";
    }

    /**
     * Called Event Handling Method
     *
     * @param weather A string representing the weather. Can be either: "normal", "cloud", "rain", or "sunny"
     */
    public abstract void handleEvent(String weather);
}
