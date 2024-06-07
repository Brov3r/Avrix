package com.avrix.events;

/**
 * Triggered when a chat message from the server admin is being sent.
 */
public abstract class OnAdminMessageEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnAdminMessage";
    }

    /**
     * Called Event Handling Method
     *
     * @param text The text of the message being received from the admin.
     * @param x    The x coordinate where to display the message.
     * @param y    The y coordinate where to display the message.
     * @param z    The z coordinate where to display the message.
     */
    public abstract void handleEvent(String text, Integer x, Integer y, Integer z);
}
