package com.avrix.events;

/**
 * Triggered when a standard group of Lua files is loaded.
 */
public abstract class OnLuaFilesLoadedEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "onLuaFilesLoaded";
    }

    /**
     * Called Event Handling Method
     *
     * @param groupName Lua file group names: shared, client or server
     */
    public abstract void handleEvent(String groupName);
}
