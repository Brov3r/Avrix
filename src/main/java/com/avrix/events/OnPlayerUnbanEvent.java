package com.avrix.events;

/**
 * Triggered when the player unban command is called.
 */
public abstract class OnPlayerUnbanEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "onPlayerUnban";
    }

    /**
     * Called Event Handling Method
     *
     * @param player    Player nickname, in case of unban by nickname,
     *                  or string representation of SteamID, in case of unban by ID
     * @param adminName Nickname of the administrator who unbanned
     */
    public abstract void handleEvent(String player, String adminName);
}
