package com.avrix.events;

/**
 * Triggered when the client global object system is being initialized.
 */
public abstract class OnCGlobalObjectSystemInitEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnCGlobalObjectSystemInit";
    }

    /**
     * Called Event Handling Method
     */
    public abstract void handleEvent();
}
