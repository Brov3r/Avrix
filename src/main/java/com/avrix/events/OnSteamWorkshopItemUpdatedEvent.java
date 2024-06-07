package com.avrix.events;

/**
 * Triggered after a Steam workship item was successfully updated.
 */
public abstract class OnSteamWorkshopItemUpdatedEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnSteamWorkshopItemUpdated";
    }

    /**
     * Called Event Handling Method
     *
     * @param userNeedsToAcceptWorkshopLegalAgreement Whether the user has to accept the workshop legal agreement.
     */
    public abstract void handleEvent(Boolean userNeedsToAcceptWorkshopLegalAgreement);
}
