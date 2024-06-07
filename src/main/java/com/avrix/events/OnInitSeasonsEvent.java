package com.avrix.events;

import zombie.erosion.season.ErosionSeason;

/**
 * Triggered when the seasons have been initialized.
 */
public abstract class OnInitSeasonsEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnInitSeasons";
    }

    /**
     * Called Event Handling Method
     *
     * @param erosionSeason The season to be initialized.
     */
    public abstract void handleEvent(ErosionSeason erosionSeason);
}
