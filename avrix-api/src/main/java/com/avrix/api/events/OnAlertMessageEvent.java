package com.avrix.api.events;

import zombie.chat.ChatMessage;

/**
 * @see OnAddMessageEvent
 */
public abstract class OnAlertMessageEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnAlertMessage";
    }

    /**
     * Called Event Handling Method
     *
     * @param message The message that was added.
     * @param tabId   The ID of the tab the message was added to.
     */
    public abstract void handle(ChatMessage message, Short tabId);
}
