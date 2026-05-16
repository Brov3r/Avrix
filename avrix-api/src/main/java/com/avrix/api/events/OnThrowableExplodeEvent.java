package com.avrix.api.events;

import zombie.iso.IsoGridSquare;
import zombie.iso.objects.IsoTrap;

/**
 * Fires when a throwable or trap explodes.
 */
public abstract class OnThrowableExplodeEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnThrowableExplode";
    }

    /**
     * Called Event Handling Method
     *
     * @param throwable The explosive.
     * @param square    The square it exploded on.
     */
    public abstract void handle(IsoTrap throwable, IsoGridSquare square);
}
