package com.avrix.api.events;

/**
 * Fires when receiving an update about the server's Steam Workshop items while connecting.
 */
public abstract class OnServerWorkshopItemsEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnServerWorkshopItems";
    }

    /**
     * Called Event Handling Method
     *
     * @param type todo
     */
    public abstract void handle(String type);
}
