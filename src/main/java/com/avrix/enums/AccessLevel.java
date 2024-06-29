package com.avrix.enums;

/**
 * Enumerates various access levels with priorities for comparison.
 */
public enum AccessLevel {
    /**
     * Highest access level, typically reserved for administrators.
     * This level has the highest priority.
     */
    ADMIN("admin", 5),

    /**
     * Access level for moderators, who manage user interactions and content.
     * Priority is lower than ADMIN but higher than OVERSEER.
     */
    MODERATOR("moderator", 4),

    /**
     * Access level for overseers, who have oversight capabilities,
     * but fewer privileges than moderators.
     */
    OVERSEER("overseer", 3),

    /**
     * Access level for game masters or GMs, who manage game-specific elements.
     * Priority is above OBSERVER but below OVERSEER.
     */
    GM("gm", 2),

    /**
     * Observer access level, typically for users with limited privileges,
     * mainly to view or monitor without broader administrative rights.
     */
    OBSERVER("observer", 1),

    /**
     * The default access level representing no special privileges.
     * This is the lowest priority level.
     */
    NONE("none", 0);

    /**
     * Role name
     */
    private final String roleName;
    
    /**
     * Access Level Priority
     */
    private final int priority;

    /**
     * Constructs an AccessLevel enum with the specified role name and priority.
     *
     * @param roleName the name of the role
     * @param priority the priority of the role for comparison purposes
     */
    AccessLevel(String roleName, int priority) {
        this.roleName = roleName;
        this.priority = priority;
    }

    /**
     * Returns the priority of a given access level.
     *
     * @return access level priority
     */
    public int getPriority() {
        return this.priority;
    }

    /**
     * Converts a string to the corresponding AccessLevel. Returns NONE if no match is found.
     *
     * @param text the string to convert
     * @return the corresponding AccessLevel
     */
    public static AccessLevel fromString(String text) {
        for (AccessLevel level : AccessLevel.values()) {
            if (level.name().equalsIgnoreCase(text)) {
                return level;
            }
        }
        return NONE;
    }
}