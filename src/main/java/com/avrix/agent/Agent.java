package com.avrix.agent;

import java.io.File;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.jar.JarFile;

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

    /**
     * Adds a JAR file to the system classpath.
     *
     * @param jarFile the JAR file to be added to the classpath.
     * @throws IllegalStateException     if the instrumentation agent is not initialized.
     * @throws NoSuchMethodException     if the method to add URL to classloader is not found.
     * @throws IOException               if there is an I/O error while accessing the JAR file.
     * @throws InvocationTargetException if there is an error invoking the method to add URL.
     * @throws IllegalAccessException    if access to the method to add URL is denied.
     */
    public static void addClassPath(File jarFile) throws NoSuchMethodException, IOException, InvocationTargetException, IllegalAccessException {
        ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();

        // If Java 9 or higher use Instrumentation
        if (!(systemClassLoader instanceof URLClassLoader)) {
            if (instrumentation == null) {
                throw new IllegalStateException("[!] Instrumentation agent is not initialized.");
            }
            instrumentation.appendToSystemClassLoaderSearch(new JarFile(jarFile));
            return;
        }

        // If Java 8 or below fallback to old method
        Method addUrlMethod = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
        addUrlMethod.setAccessible(true);
        addUrlMethod.invoke(systemClassLoader, jarFile.toURI().toURL());
    }
}