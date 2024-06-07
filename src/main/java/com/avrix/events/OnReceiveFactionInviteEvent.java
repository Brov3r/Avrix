package com.avrix.events;

/**
 * Triggered when a player is invited to join a faction.
 */
public abstract class OnReceiveFactionInviteEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "ReceiveFactionInvite";
    }

    /**
     * Called Event Handling Method
     *
     * @param factionName The name of the faction for which the player received an invitation.
     * @param playerName  The name of the player who's been invited to join the faction.
     */
    public abstract void handleEvent(String factionName, String playerName);
}
