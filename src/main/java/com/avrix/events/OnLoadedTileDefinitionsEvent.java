package com.avrix.events;

import zombie.iso.sprite.IsoSpriteManager;

/**
 * Triggered after tiles definitions have been loaded.
 */
public abstract class OnLoadedTileDefinitionsEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnLoadedTileDefinitions";
    }

    /**
     * Called Event Handling Method
     *
     * @param spriteManager The sprite manager.
     */
    public abstract void handleEvent(IsoSpriteManager spriteManager);
}
