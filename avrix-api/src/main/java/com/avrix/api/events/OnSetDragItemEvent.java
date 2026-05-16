package com.avrix.api.events;

import se.krka.kahlua.vm.KahluaTable;

/**
 * TODO: Description
 */
public abstract class OnSetDragItemEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "SetDragItem";
    }

    /**
     * Called Event Handling Method
     *
     * @param item      todo
     * @param playerNum todo
     */
    public abstract void handle(KahluaTable item, Integer playerNum);
}
