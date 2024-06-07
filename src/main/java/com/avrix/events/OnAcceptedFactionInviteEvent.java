package com.avrix.events;

/**
 * Triggered when a faction invite has been accepted.
 */
public abstract class OnAcceptedFactionInviteEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "AcceptedFactionInvite";
    }

    /**
     * Called Event Handling Method
     *
     * @param factionName The name of the faction the player accepted to join.
     * @param playerName  The name of the player who accepted the invitation.
     */
    public abstract void handleEvent(String factionName, String playerName);
}
