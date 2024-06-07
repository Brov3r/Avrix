package com.avrix.events;

/**
 * Triggered when a Steam workshop item couldn't be created.
 */
public abstract class OnSteamWorkshopItemNotCreatedEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnSteamWorkshopItemNotCreated";
    }

    /**
     * Called Event Handling Method
     *
     * @param result The result code indicating why the workshop item was not created.
     */
    public abstract void handleEvent(Integer result);
}
