package com.avrix.events;

import se.krka.kahlua.vm.KahluaTable;

/**
 * TODO
 */
public abstract class OnSteamRulesRefreshCompleteEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnSteamRulesRefreshComplete";
    }

    /**
     * Called Event Handling Method
     *
     * @param host       TODO
     * @param port       TODO
     * @param rulesTable TODO
     */
    public abstract void handleEvent(String host, Integer port, KahluaTable rulesTable);
}
