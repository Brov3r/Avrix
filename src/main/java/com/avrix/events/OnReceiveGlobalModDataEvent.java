package com.avrix.events;

import se.krka.kahlua.vm.KahluaTable;

/**
 * Triggered when the game client is receiving GlobalModData from the server.
 */
public abstract class OnReceiveGlobalModDataEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnReceiveGlobalModData";
    }

    /**
     * Called Event Handling Method
     *
     * @param key     The key for the ModData that has been received.
     * @param modData The ModData that has been received.
     */
    public abstract void handleEvent(String key, KahluaTable modData);
}
