package com.avrix.events;

import zombie.chat.ChatTab;

/**
 * Triggered when the default chat tab has been set.
 */
public abstract class OnSetDefaultTabEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnSetDefaultTab";
    }

    /**
     * Called Event Handling Method
     *
     * @param chatTab The chat tab that is being set as default.
     */
    public abstract void handleEvent(ChatTab chatTab);
}
