package com.avrix.enums;

/**
 * Enum representing the types of keyboard events.
 */
public enum KeyEventType {
    /**
     * Represents a key press event.
     * This event is triggered when a key is initially pressed down.
     */
    PRESS,

    /**
     * Represents a key repeat event.
     * This event is triggered when a key is held down, and it continuously
     * generates events after a short delay.
     */
    REPEAT,

    /**
     * Represents a key release event.
     * This event is triggered when a key is released after being pressed.
     */
    RELEASE
}
