package com.avrix.agent;

import com.avrix.Launcher;
import com.sun.tools.attach.VirtualMachine;

import java.io.File;
import java.lang.management.ManagementFactory;

/**
 * The AgentLoader class provides a method for loading an agent into a running virtual machine (JVM).
 */
public class AgentLoader {
    /**
     * The loadAgent method loads the agent into the current JVM if it is not already loaded.
     * To load the agent, the tools.jar library and classes from the {@link com.sun.tools.attach} package are used.
     * The path to the agent JAR file is determined from the location of the {@link Launcher} class that called the loadAgent method.
     * The resulting path is passed to the loadAgent method of the {@link VirtualMachine} class to load the agent.
     * After the agent is loaded, the detach method is called to detach from the virtual machine.
     *
     * @param agentJarPath path to the Jar file with this agent
     */
    public static void loadAgent(String agentJarPath) {
        if (Agent.instrumentation != null) return;

        try {
            String nameOfRunningVM = ManagementFactory.getRuntimeMXBean().getName();
            String pid = nameOfRunningVM.substring(0, nameOfRunningVM.indexOf('@'));

            VirtualMachine vm = VirtualMachine.attach(pid);
            vm.loadAgent(agentJarPath, "");
            vm.detach();
        } catch (Exception e) {
            System.err.println("[!] Error loading agent: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * The loadAgent method loads the agent into the current JVM if it is not already loaded.
     * To load the agent, the tools.jar library and classes from the {@link com.sun.tools.attach} package are used.
     * The path to the agent JAR file is determined from the location of the {@link Launcher} class that called the loadAgent method.
     * The resulting path is passed to the loadAgent method of the {@link VirtualMachine} class to load the agent.
     * After the agent is loaded, the detach method is called to detach from the virtual machine.
     */
    public static void loadAgent() {
        try {
            loadAgent(new File(Agent.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getPath());
        } catch (Exception e) {
            System.err.println("[!] Error loading agent: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
}