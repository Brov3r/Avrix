package com.avrix.events;

/**
 * Triggered when a chat tab is added.
 */
public abstract class OnTabAddedEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnTabAdded";
    }

    /**
     * Called Event Handling Method
     *
     * @param tabTitle The name of the chat tab which was added.
     * @param tabId    The identifier of the chat tab which was added.
     */
    public abstract void handleEvent(String tabTitle, Integer tabId);
}
