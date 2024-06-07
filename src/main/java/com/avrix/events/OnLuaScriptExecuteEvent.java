package com.avrix.events;

/**
 * Triggered when Lua script loaded.
 */
public abstract class OnLuaScriptExecuteEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "onLuaScriptExecute";
    }

    /**
     * Called Event Handling Method
     *
     * @param filePath        Path to Lua file
     * @param isRewriteEvents Flag indicating whether Lua events have been overwritten
     */
    public abstract void handleEvent(String filePath, boolean isRewriteEvents);
}
