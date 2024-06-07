package com.avrix.events;

import zombie.iso.IsoObject;

/**
 * Triggered when a sound is being played.
 */
public abstract class OnWorldSoundEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnWorldSound";
    }

    /**
     * Called Event Handling Method
     *
     * @param x      The x coordinate of the sound.
     * @param y      The y coordinate of the sound.
     * @param z      The z coordinate of the sound.
     * @param radius The radius of the sound.
     * @param volume The volume of the sound.
     * @param source The object that triggered the sound.
     */
    public abstract void handleEvent(Integer x, Integer y, Integer z, Integer radius, Integer volume, IsoObject source);
}
