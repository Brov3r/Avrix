package com.avrix.lua;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * A utility class for exposing classes and global objects for use in Lua scripts.
 */
public class LuaExposer {
    /**
     * A collection of classes that should be exhibited.
     */
    private static final Set<Class<?>> exposedClasses = new HashSet<>();

    /**
     * A collection of global objects that should be exposed.
     */
    private static final Set<Object> exposedGlobalObjects = new HashSet<>();

    /**
     * Method for exposing the class.
     *
     * @param clazz The {@link Class} to expose.
     */
    public static synchronized void addExposedClass(Class<?> clazz) {
        exposedClasses.add(clazz);
    }

    /**
     * Method for exposing a global object, which contain methods with annotation {@link se.krka.kahlua.integration.annotations.LuaMethod}.
     *
     * @param globalObject The global {@link Object} to expose.
     */
    public static synchronized void addExposedGlobalObject(Object globalObject) {
        exposedGlobalObjects.add(globalObject);
    }

    /**
     * Method for getting a collection of exposed classes.
     *
     * @return A {@link Set} of exposed classes.
     */
    public static synchronized Set<Class<?>> getExposedClasses() {
        return Collections.unmodifiableSet(exposedClasses);
    }

    /**
     * Method for getting a collection of exposed global objects.
     *
     * @return A {@link Set} of exposed global objects.
     */
    public static synchronized Set<Object> getExposedGlobalObjects() {
        return Collections.unmodifiableSet(exposedGlobalObjects);
    }

    /**
     * Method for removing a class from the collection of exposed classes.
     *
     * @param clazz The {@link Class} to delete.
     */
    public static synchronized void removeExposedClass(Class<?> clazz) {
        exposedClasses.remove(clazz);
    }

    /**
     * Method for removing a global object from the collection of exposed global objects.
     *
     * @param globalObject The global {@link Object} to delete.
     */
    public static synchronized void removeExposedGlobalObject(Object globalObject) {
        exposedGlobalObjects.remove(globalObject);
    }
}