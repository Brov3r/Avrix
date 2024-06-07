package com.avrix.events;

/**
 * Triggered when Lua is being reset.
 */
public abstract class OnResetLuaEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnResetLua";
    }

    /**
     * Called Event Handling Method
     *
     * @param reason The reason why Lua was reset.
     */
    public abstract void handleEvent(String reason);
}
