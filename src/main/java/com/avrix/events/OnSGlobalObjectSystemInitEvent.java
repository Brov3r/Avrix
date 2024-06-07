package com.avrix.events;

/**
 * Triggered when the server global object system is being initialized.
 */
public abstract class OnSGlobalObjectSystemInitEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnSGlobalObjectSystemInit";
    }

    /**
     * Called Event Handling Method
     */
    public abstract void handleEvent();
}
