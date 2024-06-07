package com.avrix.events;

import se.krka.kahlua.vm.KahluaTable;

/**
 * Triggered when a command from the server is being received.
 */
public abstract class OnServerCommandEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnServerCommand";
    }

    /**
     * Called Event Handling Method
     *
     * @param module    The name of the module for this server command.
     * @param command   The text of the actual server command.
     * @param arguments The list of arguments of the server command.
     */
    public abstract void handleEvent(String module, String command, KahluaTable arguments);
}
