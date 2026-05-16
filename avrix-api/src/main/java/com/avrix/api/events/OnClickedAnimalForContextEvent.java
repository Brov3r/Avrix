package com.avrix.api.events;

import se.krka.kahlua.vm.KahluaTable;
import zombie.characters.animals.IsoAnimal;

/**
 * TODO: Description
 */
public abstract class OnClickedAnimalForContextEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnClickedAnimalForContext";
    }

    /**
     * Called Event Handling Method
     *
     * @param playerNum todo
     * @param context   todo
     * @param animal    todo
     * @param test      todo
     */
    public abstract void handle(Integer playerNum, KahluaTable context, IsoAnimal animal, Boolean test);
}
