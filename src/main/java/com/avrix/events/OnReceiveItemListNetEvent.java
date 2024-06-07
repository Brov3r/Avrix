package com.avrix.events;

import zombie.characters.IsoPlayer;

import java.util.ArrayList;

/**
 * Triggered when a player is receiving a list of items from another player.
 */
public abstract class OnReceiveItemListNetEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnReceiveItemListNet";
    }

    /**
     * Called Event Handling Method
     *
     * @param sender    The player who's sending the item list.
     * @param itemList  The list of items that is being received.
     * @param receiver  The player who's receiving the item list.
     * @param sessionId The session identifier for the transaction.
     * @param custom    TODO
     */
    public abstract void handleEvent(IsoPlayer sender, ArrayList itemList, IsoPlayer receiver, String sessionId, String custom);
}
