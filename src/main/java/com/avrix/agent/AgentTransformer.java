package com.avrix.agent;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

/**
 * The class represents a class transformer, used to change classes at boot time.
 * It implements the {@link ClassFileTransformer} interface, which allows you to change the byte code of classes before they are loaded into the JVM.
 */
public class AgentTransformer implements ClassFileTransformer {
    /**
     * The transform method is called for each class that is loaded into the JVM.
     * It takes the byte code of the original class and returns the modified byte code.
     * This method implementation calls the getModifyClass method of the {@link AgentManager} class to obtain the modified byte code of the class.
     *
     * @param loader              class of the loader that loads this class
     * @param className           full class name in class path format (delimited by '/')
     * @param classBeingRedefined the class that is being overridden (can be null if the class is not being overridden)
     * @param protectionDomain    the protection domain in which the class runs
     * @param classfileBuffer     byte code of the original class
     * @return the modified byte code of the class
     * @throws IllegalClassFormatException if the class bytecode cannot be transformed
     */
    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        return AgentManager.getModifyClass(className.replaceAll("/", "."));
    }
}