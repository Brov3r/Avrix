package com.avrix.api.events;

/**
 * Fires before the foraging system processes trait and profession definitions.
 */
public abstract class OnPreAddSkillDefsEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "preAddSkillDefs";
    }

    /**
     * Called Event Handling Method
     *
     * @param system The foraging system.
     */
    public abstract void handle(Object system);
}
