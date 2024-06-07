package com.avrix.events;

import se.krka.kahlua.vm.KahluaTable;

/**
 * TODO
 */
public abstract class OnFillSearchIconContextMenuEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "onFillSearchIconContextMenu";
    }

    /**
     * Called Event Handling Method
     *
     * @param context  The context menu to be filled.
     * @param baseIcon TODO
     */
    public abstract void handleEvent(KahluaTable context, KahluaTable baseIcon);
}
