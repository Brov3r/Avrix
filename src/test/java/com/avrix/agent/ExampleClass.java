package com.avrix.agent;

/**
 * Example class with a method that can be patched.
 */
public class ExampleClass {
    /**
     * A method that returns false. This method will be patched in tests to change its behavior.
     *
     * @return false always
     */
    public static boolean getTest() {
        return false;
    }
}