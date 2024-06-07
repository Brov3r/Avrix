package com.avrix.events;

/**
 * Triggered when an ambient sound starts.
 */
public abstract class OnAmbientSoundEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnAmbientSound";
    }

    /**
     * Called Event Handling Method
     *
     * @param name The name of the ambient sound.
     * @param x    The x coordinate of the ambient sound.
     * @param y    The y coordinate of the ambient sound.
     */
    public abstract void handleEvent(String name, Float x, Float y);
}
