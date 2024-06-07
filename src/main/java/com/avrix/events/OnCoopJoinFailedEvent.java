package com.avrix.events;

/**
 * Triggered when a player fails to join a cooperative game.
 */
public abstract class OnCoopJoinFailedEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnCoopJoinFailed";
    }

    /**
     * Called Event Handling Method
     *
     * @param playerId The identifier of the player who was denied access to join the cooperative game. It can be either 0, 1, 2, or 3.
     */
    public abstract void handleEvent(Integer playerId);
}
