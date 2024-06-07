package com.avrix.events;

/**
 * Triggered when the chat window is being initialized.
 */
public abstract class OnChatWindowInitEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnChatWindowInit";
    }

    /**
     * Called Event Handling Method
     */
    public abstract void handleEvent();
}
