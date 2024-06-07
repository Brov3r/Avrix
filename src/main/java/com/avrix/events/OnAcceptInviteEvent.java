package com.avrix.events;

/**
 * Triggered when a Steam invite has been accepted.
 */
public abstract class OnAcceptInviteEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnAcceptInvite";
    }

    /**
     * Called Event Handling Method
     *
     * @param connectionString The connection string.
     */
    public abstract void handleEvent(String connectionString);
}
