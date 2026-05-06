package com.avrix.api.events;

import se.krka.kahlua.vm.KahluaTable;

/**
 * Triggered when the game client is receiving inventory items from the server.
 */
public abstract class OnMngInvReceiveItemsEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "MngInvReceiveItems";
    }

    /**
     * Called Event Handling Method
     *
     * @param items The items that are being received.
     */
    public abstract void handle(KahluaTable items);
}
