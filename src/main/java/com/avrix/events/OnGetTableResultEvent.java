package com.avrix.events;

import java.util.ArrayList;

/**
 * Triggered when the game client is receiving a table result from the server.
 */
public abstract class OnGetTableResultEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnGetTableResult";
    }

    /**
     * Called Event Handling Method
     *
     * @param result    The row data of the table result.
     * @param rowId     The row identifier of the table result.
     * @param tableName The name of the table result.
     */
    public abstract void handleEvent(ArrayList result, Integer rowId, String tableName);
}
