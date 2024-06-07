package com.avrix.events;

/**
 * Implementing a test event
 */
public class TestEventWithExceptionImpl extends TestEvent {
    /**
     * Called Event Handling Method
     *
     * @param testString  Test argument
     * @param testInteger Test argument
     */
    @Override
    public void handleEvent(String testString, Integer testInteger) {
        throw new RuntimeException("Test exception");
    }

    /**
     * Called Event Handling Method
     */
    @Override
    public void handleEvent() {
        throw new RuntimeException("Another exception test");
    }

    /**
     * Implementing a test event name
     */
    @Override
    public String getEventName() {
        return "OnTestEventException";
    }
}
