package com.avrix.events;

import zombie.iso.areas.SafeHouse;

/**
 * Triggered when a player is invited to a safehouse.
 */
public abstract class OnReceiveSafehouseInviteEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "ReceiveSafehouseInvite";
    }

    /**
     * Called Event Handling Method
     *
     * @param safeHouse  The safehouse for which the player received an invitation.
     * @param playerName The name of the player who's been invited to join the safehouse.
     */
    public abstract void handleEvent(SafeHouse safeHouse, String playerName);
}
