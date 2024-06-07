package com.avrix.events;

/**
 * Triggered after a new Steam workshop item was successfully created.
 */
public abstract class OnSteamWorkshopItemCreatedEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnSteamWorkshopItemCreated";
    }

    /**
     * Called Event Handling Method
     *
     * @param steamId                                 The Steam identifier of the user who created the workshop item.
     * @param userNeedsToAcceptWorkshopLegalAgreement Whether the user has to accept the workshop legal agreement.
     */
    public abstract void handleEvent(String steamId, Boolean userNeedsToAcceptWorkshopLegalAgreement);
}
