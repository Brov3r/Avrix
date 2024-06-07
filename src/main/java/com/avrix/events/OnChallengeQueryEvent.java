package com.avrix.events;

/**
 * Triggered when the challenge options are getting populated.
 */
public abstract class OnChallengeQueryEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnChallengeQuery";
    }

    /**
     * Called Event Handling Method
     */
    public abstract void handleEvent();
}
