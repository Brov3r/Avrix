package com.avrix.events;

/**
 * Triggered when a safehouse invite has been accepted.
 */
public abstract class OnAcceptedSafehouseInviteEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "AcceptedSafehouseInvite";
    }

    /**
     * Called Event Handling Method
     *
     * @param safehouseName The name of the safehouse the player accepted to join.
     * @param playerName    The name of the player who accepted the invitation.
     */
    public abstract void handleEvent(String safehouseName, String playerName);
}
