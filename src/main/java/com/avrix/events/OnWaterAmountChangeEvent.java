package com.avrix.events;

import zombie.iso.IsoObject;

/**
 * Triggered when the amount of water in an object has changed.
 */
public abstract class OnWaterAmountChangeEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnWaterAmountChange";
    }

    /**
     * Called Event Handling Method
     *
     * @param object      The object in which the water amount is changing.
     * @param waterAmount The amount of water that is being added or removed from the water container.
     */
    public abstract void handleEvent(IsoObject object, Integer waterAmount);
}
