package com.avrix.events;

import zombie.characters.IsoGameCharacter;
import zombie.inventory.InventoryItem;
import zombie.inventory.types.Moveable;
import zombie.scripting.objects.MovableRecipe;

/**
 * Triggered when a dynamic recipe for a movable inventory item is being used.
 */
public abstract class OnDynamicMovableRecipeEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnDynamicMovableRecipe";
    }

    /**
     * Called Event Handling Method
     *
     * @param moveable      The movable object resulting from using the recipe.
     * @param movableRecipe The movable recipe that is being used.
     * @param inventoryItem The item from the player inventory.
     * @param character     The character who's using the recipe.
     */
    public abstract void handleEvent(Moveable moveable, MovableRecipe movableRecipe, InventoryItem inventoryItem, IsoGameCharacter character);
}
