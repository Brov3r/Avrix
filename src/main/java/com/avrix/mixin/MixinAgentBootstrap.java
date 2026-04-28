package com.avrix.mixin;

import com.sun.tools.attach.VirtualMachine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.net.URISyntaxException;

/**
 * Bootstrap to dynamically load the Java agent via the Attach API.
 * <p>
 * Allows you to load {@link MixinAgent} into the current or external JVM process
 * without the {@code -javaagent} flag. Requires {@code -Djdk.attach.allowAttachSelf=true}
 * when run to attach to itself.
 */
public class MixinAgentBootstrap {
    private static final Logger log = LoggerFactory.getLogger(MixinAgentBootstrap.class);

    /**
     * Loads the agent into the specified JVM process.
     *
     * @param agentJarPath absolute path to the agent's JAR file
     * @throws RuntimeException if the connection or agent loading fails
     */
    public synchronized static void loadAgent(String agentJarPath) {
        if (MixinAgent.isAttached()) return;

        try {
            String nameOfRunningVM = ManagementFactory.getRuntimeMXBean().getName();
            String pid = nameOfRunningVM.substring(0, nameOfRunningVM.indexOf('@'));

            VirtualMachine vm = VirtualMachine.attach(pid);
            vm.loadAgent(agentJarPath, "");
            vm.detach();
        } catch (Exception e) {
            log.error("Loading of the mixin agent failed!", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Loads the agent from the current class location.
     * Convenient overloaded method for when the agent is in the same JAR as {@link MixinAgent}.
     *
     * @throws URISyntaxException if the path to the agent could not be determined or loaded
     */
    public synchronized static void loadAgent() throws URISyntaxException {
        loadAgent(new File(MixinAgent.class.getProtectionDomain()
                .getCodeSource()
                .getLocation()
                .toURI()).getPath());
    }
}