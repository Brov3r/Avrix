package com.avrix.events;

import se.krka.kahlua.vm.KahluaTable;

/**
 * Triggered after spawn regions are loaded.
 * This event is triggered on the server.
 */
public abstract class OnSpawnRegionsLoadedEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnSpawnRegionsLoaded";
    }

    /**
     * Called Event Handling Method
     *
     * @param spawnRegions A table of spawn regions that have been loaded.
     */
    public abstract void handleEvent(KahluaTable spawnRegions);
}
