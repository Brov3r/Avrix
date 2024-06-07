package com.avrix.events;

/**
 * Triggered when the player receives a server message during a cooperative game.
 */
public abstract class OnCoopServerMessageEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnCoopServerMessage";
    }

    /**
     * Called Event Handling Method
     *
     * @param messageType The type of message received from the server. Can be either `ping`, `pong`, `steam-id`, or `server-address`.
     * @param playerNick  The nick of the player who's sending the message.
     * @param steamId     The Steam identifier of the player who's sending the message.
     */
    public abstract void handleEvent(String messageType, String playerNick, String steamId);
}
