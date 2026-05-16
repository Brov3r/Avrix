package com.avrix.api.events;

import se.krka.kahlua.vm.KahluaTable;
import zombie.characters.IsoPlayer;

/**
 * TODO: Description
 */
public abstract class OnProcessActionEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnProcessAction";
    }

    /**
     * Called Event Handling Method
     *
     * @param builderMethod todo
     * @param player        todo
     * @param argTable      todo
     */
    public abstract void handle(String builderMethod, IsoPlayer player, KahluaTable argTable);
}
