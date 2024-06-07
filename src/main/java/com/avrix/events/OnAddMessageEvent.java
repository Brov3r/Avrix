package com.avrix.events;

import zombie.chat.ChatMessage;

/**
 * Triggered when a chat message is being sent.
 */
public abstract class OnAddMessageEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnAddMessage";
    }

    /**
     * Called Event Handling Method
     *
     * @param chatMessage The chat message being added.
     * @param tabId       The identifier of the tab in which the message is being added.
     */
    public abstract void handleEvent(ChatMessage chatMessage, Short tabId);
}
