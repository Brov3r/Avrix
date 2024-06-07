package com.avrix.events;

import se.krka.kahlua.vm.KahluaTable;

/**
 * Triggered when the game client is receiving a database schema from the server.
 */
public abstract class OnGetDBSchemaEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnGetDBSchema";
    }

    /**
     * Called Event Handling Method
     *
     * @param dbSchema The database schema of the game client.
     */
    public abstract void handleEvent(KahluaTable dbSchema);
}
