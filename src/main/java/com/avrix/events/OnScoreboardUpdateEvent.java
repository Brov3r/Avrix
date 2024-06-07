package com.avrix.events;

import java.util.ArrayList;

/**
 * Triggered when multiplayer scoreboard is updated.
 */
public abstract class OnScoreboardUpdateEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnScoreboardUpdate";
    }

    /**
     * Called Event Handling Method
     *
     * @param playerNames  The list of player names which are being updated on the score board.
     * @param displayNames The list of display names for the players which are being updated on the score board.
     * @param steamIds     The list of Steam identifiers of the players which are being updated on the score board.
     */
    public abstract void handleEvent(ArrayList playerNames, ArrayList displayNames, ArrayList steamIds);
}
