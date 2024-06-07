package com.avrix.events;

/**
 * Triggered when the command is sent to the server console.
 */
public abstract class OnSendConsoleCommandEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "onSendConsoleCommand";
    }

    /**
     * Called Event Handling Method
     *
     * @param command command sent to the console including arguments, i.e. the entire string sent to the console
     */
    public abstract void handleEvent(String command);
}
