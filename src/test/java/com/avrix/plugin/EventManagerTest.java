package com.avrix.plugin;

import com.avrix.events.EventPriority;
import com.avrix.events.TestEventHighPriorityImpl;
import com.avrix.events.TestEventImpl;
import com.avrix.events.TestEventWithExceptionImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the {@link EventManager} class.
 */
public class EventManagerTest {
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;

    /**
     * Redirects System.out to capture console output.
     */
    @BeforeEach
    public void setUpStreams() {
        System.setOut(new PrintStream(outContent));
    }

    /**
     * Restores System.out after each test and clears all event listeners.
     */
    @AfterEach
    public void restoreStreams() {
        System.setOut(originalOut);
        EventManager.clearAllListeners();
    }

    /**
     * Tests the {@link EventManager#clearAllListeners()} method.
     */
    @Test
    public void testClearAllListeners() {
        EventManager.addListener(new TestEventImpl());

        assertNotNull(EventManager.getAllListeners());
        assertNotNull(EventManager.getListenersForEvent("OnTestEvent"));

        assertEquals(1, EventManager.getAllListeners().size());
        assertEquals(1, EventManager.getAllListeners().get("OnTestEvent").size());
        assertEquals(1, EventManager.getListenersForEvent("OnTestEvent").size());

        EventManager.clearAllListeners();
        EventManager.invokeEvent("OnTestEvent");

        assertEquals("", outContent.toString().trim());
        assertTrue(EventManager.getAllListeners().isEmpty());
    }

    /**
     * Tests the {@link EventManager#clearListenersForEvent(String)} method.
     */
    @Test
    public void testClearListenersForEvent() {
        EventManager.addListener(new TestEventImpl());

        assertNotNull(EventManager.getAllListeners());
        assertNotNull(EventManager.getListenersForEvent("OnTestEvent"));

        assertEquals(1, EventManager.getAllListeners().size());
        assertEquals(1, EventManager.getAllListeners().get("OnTestEvent").size());
        assertEquals(1, EventManager.getListenersForEvent("OnTestEvent").size());

        EventManager.clearListenersForEvent("OnTestEvent");
        EventManager.invokeEvent("OnTestEvent");

        assertEquals("", outContent.toString().trim());
        assertNull(EventManager.getListenersForEvent("OnTestEvent"));
        assertTrue(EventManager.getAllListeners().isEmpty());
    }

    /**
     * Tests event invocation using the {@link EventManager#invokeEvent(String, Object...)} method.
     */
    @Test
    public void testEvent() {
        EventManager.addListener(new TestEventImpl());

        assertNotNull(EventManager.getAllListeners());
        assertNotNull(EventManager.getListenersForEvent("OnTestEvent"));

        assertEquals(1, EventManager.getAllListeners().size());
        assertEquals(1, EventManager.getAllListeners().get("OnTestEvent").size());
        assertEquals(1, EventManager.getListenersForEvent("OnTestEvent").size());

        EventManager.invokeEvent("OnTestEvent");
        EventManager.invokeEvent("OnTestEvent", "Example text", 1337);

        String expectedOutput = "[#] Test event!";
        String expectedOutputWithArgs = "[#] Test event! String: Example text, Integer: 1337";

        String[] lines = outContent.toString().split("\\r?\\n");

        assertEquals(expectedOutput, lines[0].trim());
        assertEquals(expectedOutputWithArgs, lines[1].trim());
    }

    /**
     * Tests event invocation with different listener priorities.
     */
    @Test
    public void testEventPriority() {
        EventManager.addListener(new TestEventImpl(), EventPriority.LOWEST);
        EventManager.addListener(new TestEventHighPriorityImpl(), EventPriority.HIGHEST);

        assertNotNull(EventManager.getAllListeners());
        assertNotNull(EventManager.getListenersForEvent("OnTestEvent"));

        assertEquals(1, EventManager.getAllListeners().size());
        assertEquals(2, EventManager.getAllListeners().get("OnTestEvent").size());
        assertEquals(2, EventManager.getListenersForEvent("OnTestEvent").size());

        EventManager.invokeEvent("OnTestEvent");

        String expectedOutput = "[#] Test event high priority!";
        String expectedOutputWithArgs = "[#] Test event!";

        String[] lines = outContent.toString().split("\\r?\\n");

        assertEquals(expectedOutput, lines[0].trim());
        assertEquals(expectedOutputWithArgs, lines[1].trim());
    }

    /**
     * Tests event invocation with listeners that may throw exceptions.
     */
    @Test
    public void testEventWithException() {
        EventManager.addListener(new TestEventWithExceptionImpl());

        assertNotNull(EventManager.getAllListeners());
        assertNotNull(EventManager.getListenersForEvent("OnTestEventException"));

        assertEquals(1, EventManager.getAllListeners().size());
        assertEquals(1, EventManager.getAllListeners().get("OnTestEventException").size());
        assertEquals(1, EventManager.getListenersForEvent("OnTestEventException").size());

        EventManager.invokeEvent("OnTestEventException", "Example text", 1234, true);
        EventManager.invokeEvent("OnTestEventException", "Example text", 1234);
        EventManager.invokeEvent("OnTestEventException");

        String noSuchMethodExceptionText = "[!] Compatible 'handleEvent' method not found for event 'OnTestEventException' in listener '"
                + TestEventWithExceptionImpl.class + "'. Argument types: 'String, Integer, Boolean'";

        String exceptionTextFirst = "[!] An exception occurred when trying to invoke event 'OnTestEventException' " +
                "with arguments 'String, Integer' in listener '" + TestEventWithExceptionImpl.class + "'! Reason: Test exception";

        String exceptionTextSecond = "[!] An exception occurred when trying to invoke event 'OnTestEventException' " +
                "with arguments '' in listener '" + TestEventWithExceptionImpl.class + "'! Reason: Another exception test";

        String[] lines = outContent.toString().split("\\r?\\n");
        assertEquals(noSuchMethodExceptionText, lines[0].trim());
        assertEquals(exceptionTextFirst, lines[1].trim());
        assertEquals(exceptionTextSecond, lines[2].trim());
    }
}