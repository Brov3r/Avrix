package com.avrix.api.events;

/**
 * TODO: Description
 */
public abstract class OnTemplateTextInitEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnTemplateTextInit";
    }

    /**
     * Called Event Handling Method
     */
    public abstract void handle();
}
