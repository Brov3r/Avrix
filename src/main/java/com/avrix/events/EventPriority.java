package com.avrix.events;

/**
 * The enumeration defines five event priority levels used to control the order in which events are processed in an application.
 */
public enum EventPriority {
    /**
     * Highest priority events.
     */
    HIGHEST,

    /**
     * High priority events.
     */
    HIGH,

    /**
     * Events with normal priority.
     */
    NORMAL,

    /**
     * Low priority events.
     */
    LOW,

    /**
     * Lowest priority events.
     */
    LOWEST;
}