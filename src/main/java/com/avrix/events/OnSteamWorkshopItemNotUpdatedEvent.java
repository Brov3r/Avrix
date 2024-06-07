package com.avrix.events;

/**
 * Triggered when a Steam workshop item couldn't be updated.
 */
public abstract class OnSteamWorkshopItemNotUpdatedEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnSteamWorkshopItemNotUpdated";
    }

    /**
     * Called Event Handling Method
     *
     * @param result The result code indicating why the workshop item was not updated.
     */
    public abstract void handleEvent(Integer result);
}
