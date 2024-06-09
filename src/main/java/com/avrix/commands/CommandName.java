package com.avrix.commands;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation that includes the name of the command (without slashes or other prefixes).
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface CommandName {
    /**
     * The name of the command.
     *
     * @return The name of the command without slashes or prefixes.
     */
    String value();
}