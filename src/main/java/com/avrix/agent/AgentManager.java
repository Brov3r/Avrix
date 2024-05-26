package com.avrix.agent;

import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.util.HashMap;
import java.util.Map;

/**
 * A class for managing agents designed to modify classes at runtime.
 */
public class AgentManager {
    private static final Map<String, byte[]> modifyMap = new HashMap<>(); // Repository of modified classes
    private static final AgentTransformer agentTransformer = new AgentTransformer(); // Transformer for agent

    /**
     * Method for getting a modified class by its name.
     *
     * @param className Class name
     * @return Modified class as a byte array
     */
    public static byte[] getModifyClass(String className) {
        try {
            return modifyMap.remove(className);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Method for modifying a class.
     *
     * @param className Name of the class to modify
     * @param newClass  Modified class file as a byte array
     */
    public static void modifyClass(String className, byte[] newClass) {
        try {
            Class<?> clazz = Class.forName(className);

            Instrumentation instrumentation = Agent.instrumentation;

            if (instrumentation != null && instrumentation.isModifiableClass(clazz)) {
                modifyMap.put(clazz.getCanonicalName(), newClass);
                instrumentation.addTransformer(agentTransformer, true);
                instrumentation.retransformClasses(clazz);
                instrumentation.removeTransformer(agentTransformer);
            } else {
                System.err.println("[!] Failed to modify class: " + className);
            }

        } catch (UnmodifiableClassException | ClassNotFoundException e) {
            System.err.println("[!] Failed to modify class: " + className);
            System.err.println("[!] Exception: " + e.getMessage());
            e.printStackTrace();
        }
    }
}