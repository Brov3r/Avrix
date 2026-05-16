package com.avrix.api.events;

/**
 * Fires after loading the map zones.
 */
public abstract class OnLoadedMapZonesEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnLoadedMapZones";
    }

    /**
     * Called Event Handling Method
     */
    public abstract void handle();
}
