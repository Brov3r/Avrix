package com.avrix.api.events;

/**
 * Triggered when IsoWorld initialises and is registering the zones for the map.
 */
public abstract class OnLoadMapZonesEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnLoadMapZones";
    }

    /**
     * Called Event Handling Method
     */
    public abstract void handle();
}
