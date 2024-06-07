package com.avrix.commands;

import com.avrix.enums.AccessLevel;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * An annotation that indicates the required access level for a user.
 * Users with a lower access level than specified will not have access to the functionality.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface CommandAccessLevel {
    /**
     * Specifies the required access level.
     *
     * @return The access level required for the functionality.
     * The default is AccessLevel.NONE, meaning no specific access level is required.
     */
    AccessLevel accessLevel() default AccessLevel.NONE;
}