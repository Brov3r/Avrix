package com.avrix.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Information annotation indicating that the method has been modified
 */
@Target({ElementType.METHOD})
public @interface Modified {
}