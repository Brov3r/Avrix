package com.avrix.agent;

import javassist.CannotCompileException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * This class contains tests to validate the patching functionality of the {@link ClassModifier} class.
 * It uses the Javassist library to dynamically modify bytecode of methods in the {@link ExampleClass}.
 */
public class PatchTest {

    /**
     * Tests that the {@link ExampleClass#getTest()} method is correctly patched to:
     * 1. Return {@code true} regardless of its original implementation.
     * 2. Print "Hello world!" to the console before executing the patched method body.
     * <p>
     * The test uses the {@link AgentLoader} to load the Java agent from the path specified
     * in the environment variable "AGENT_JAR_PATH".
     */
    @Test
    public void testMethodPatchedToReturnTrue() {
        // Load the Java agent from the specified path
        AgentLoader.loadAgent(System.getenv("AGENT_JAR_PATH"));

        // Create a new patcher and define the patches to be applied
        ClassModifier patch = new ClassModifier.ClassModifierBuilder("com.avrix.agent.ExampleClass")
                .modifyMethod("getTest", (ctClass, ctMethod) -> {
                    try {
                        // Patch the method body to return true
                        ctMethod.setBody("{return true;}");
                    } catch (CannotCompileException e) {
                        throw new RuntimeException(e);
                    }
                })
                .modifyMethod("getTest", (ctClass, ctMethod) -> {
                    try {
                        // Insert code to print "Hello world!" before the method body
                        ctMethod.insertBefore("{System.out.println(\"Hello world!\");}");
                    } catch (CannotCompileException e) {
                        throw new RuntimeException(e);
                    }
                }).build();

        // Apply the patches
        patch.applyModifications();

        // Assert that the patched method returns true
        Assertions.assertTrue(ExampleClass.getTest(), "getTest should return true after patching");
    }
}