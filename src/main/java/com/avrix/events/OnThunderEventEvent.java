package com.avrix.events;

/**
 * Triggered when a thunderstorm is about to start.
 */
public abstract class OnThunderEventEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnThunderEvent";
    }

    /**
     * Called Event Handling Method
     *
     * @param x      The x coordinate where the thunder event is going to take place.
     * @param y      The y coordinate where the thunder event is going to take place.
     * @param strike Whether the thunder event will strike.
     * @param light  Whether the thunder event will emit light.
     * @param rumble Whether the thunder event will rumble.
     */
    public abstract void handleEvent(Integer x, Integer y, Boolean strike, Boolean light, Boolean rumble);
}
