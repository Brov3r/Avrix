package com.avrix.commands;

import com.avrix.enums.CommandScope;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation to specify the scope where a command can be executed.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface CommandExecutionScope {
    /**
     * The scope of the command.
     *
     * @return The specified command scope.
     */
    CommandScope scope();
}