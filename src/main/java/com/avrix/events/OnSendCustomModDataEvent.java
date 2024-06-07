package com.avrix.events;

/**
 * Triggered when the game server is sending custom ModData to the client.
 */
public abstract class OnSendCustomModDataEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "SendCustomModData";
    }

    /**
     * Called Event Handling Method
     */
    public abstract void handleEvent();
}
