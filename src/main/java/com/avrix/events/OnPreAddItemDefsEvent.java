package com.avrix.events;

import se.krka.kahlua.vm.KahluaTable;

/**
 * Triggered before foraging item definitions are being added.
 */
public abstract class OnPreAddItemDefsEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "preAddItemDefs";
    }

    /**
     * Called Event Handling Method
     *
     * @param forageSystem The forage system object.
     */
    public abstract void handleEvent(KahluaTable forageSystem);
}
