package com.avrix.commands;

import com.avrix.enums.AccessLevel;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation that indicates the required access level for a user.
 * Users with a lower access level than specified will not have access to the functionality.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface CommandAccessLevel {
    /**
     * Specifies the required access level.
     *
     * @return The access level required for the functionality.
     * The default is {@link AccessLevel}.NONE, meaning no specific access level is required.
     */
    AccessLevel value() default AccessLevel.NONE;
}