package com.avrix.events;

/**
 * Triggered when a user presses TAB on his keyboard to switch chat stream.
 */
public abstract class OnSwitchChatStreamEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "SwitchChatStream";
    }

    /**
     * Called Event Handling Method
     */
    public abstract void handleEvent();
}
