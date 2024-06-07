package com.avrix.commands;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation that provides the text to be output to chat when the command is used
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface CommandChatReturn {
    /**
     * Text that will be displayed when using the command
     *
     * @return Message text
     */
    String text() default "";
}