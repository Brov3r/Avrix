package com.avrix.agent;

import java.lang.instrument.Instrumentation;

/**
 * The Agent class represents a Java instrumentation agent used for dynamic manipulation of the Java runtime environment.
 * This class allows you to set a {@link Instrumentation} object for later use in the agent.
 */
public class Agent {
    /**
     * A {@link Instrumentation} object used by the agent to access class data and load it into the JVM.
     */
    public static Instrumentation instrumentation;

    /**
     * The agentmain method is called when the agent is loaded into a running virtual machine (JVM).
     * It sets up a {@link Instrumentation} object for later use in the agent.
     *
     * @param agentArgs the agent arguments passed when it was launched
     * @param inst      {@link Instrumentation} object providing access to the JVM
     */
    public static void agentmain(String agentArgs, Instrumentation inst) {
        instrumentation = inst;
    }
}