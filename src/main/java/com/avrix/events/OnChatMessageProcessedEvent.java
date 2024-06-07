package com.avrix.events;

import zombie.chat.ChatBase;
import zombie.chat.ChatMessage;

/**
 * Triggered when a player sends a chat message.
 */
public abstract class OnChatMessageProcessedEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "onChatMessageProcessed";
    }

    /**
     * Called Event Handling Method
     *
     * @param chatBase    Target chat data.
     * @param chatMessage Sent message details.
     */
    public abstract void handleEvent(ChatBase chatBase, ChatMessage chatMessage);
}
