package com.avrix.events;

/**
 * Triggered when a mod has been modified on the filesystem.
 */
public abstract class OnModsModifiedEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnModsModified";
    }

    /**
     * Called Event Handling Method
     */
    public abstract void handleEvent();
}
