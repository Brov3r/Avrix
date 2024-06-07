package com.avrix.events;

import zombie.iso.IsoGridSquare;
import zombie.ui.ObjectTooltip;

/**
 * Triggered when a special tooltip is being rendered, after a user right-clicked an object.
 */
public abstract class OnDoSpecialTooltipEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "DoSpecialTooltip";
    }

    /**
     * Called Event Handling Method
     *
     * @param objectTooltip The tooltip object to be filled.
     * @param square        The grid square on which the tooltip has been triggered.
     */
    public abstract void handleEvent(ObjectTooltip objectTooltip, IsoGridSquare square);
}
