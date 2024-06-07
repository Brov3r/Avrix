package com.avrix.events;

/**
 * Triggered when a faction is being synced by the server on client side.
 */
public abstract class OnSyncFactionEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "SyncFaction";
    }

    /**
     * Called Event Handling Method
     *
     * @param factionName The name of the faction which is going to get synchronized.
     */
    public abstract void handleEvent(String factionName);
}
