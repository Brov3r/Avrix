package com.avrix.events;

import zombie.iso.IsoGridSquare;

/**
 * Triggered after ModData has been received from the server.
 */
public abstract class OnLoadModDataFromServerEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "onLoadModDataFromServer";
    }

    /**
     * Called Event Handling Method
     *
     * @param square The grid square whose ModData is getting loaded from the server.
     */
    public abstract void handleEvent(IsoGridSquare square);
}
