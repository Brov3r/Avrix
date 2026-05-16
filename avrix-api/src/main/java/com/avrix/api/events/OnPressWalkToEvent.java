package com.avrix.api.events;

/**
 * Fires when the local player 1 presses their Walk To keybind.
 */
public abstract class OnPressWalkToEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnPressWalkTo";
    }

    /**
     * Called Event Handling Method
     *
     * @param x todo
     * @param y todo
     * @param z todo
     */
    public abstract void handle(Integer x, Integer y, Integer z);
}
