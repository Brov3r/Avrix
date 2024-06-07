package com.avrix.events;

/**
 * Test event
 */
public abstract class TestEvent extends Event {
    /**
     * Getting the event name
     *
     * @return name of the event being implemented
     */
    @Override
    public String getEventName() {
        return "OnTestEvent";
    }

    /**
     * Called Event Handling Method
     *
     * @param testString  Test argument
     * @param testInteger Test argument
     */
    public abstract void handleEvent(String testString, Integer testInteger);

    /**
     * Called Event Handling Method
     */
    public abstract void handleEvent();
}
