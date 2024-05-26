package com.avrix.agent;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;

import java.util.function.BiConsumer;

/**
 * A class for modifying Java classes at runtime using the Javassist library.
 */
public class Patcher {
    private final CtClass ctClass; // Represents a modifiable class
    private final String className; // Path to class

    /**
     * Constructor for creating a {@link Patcher} object.
     *
     * @param builder the {@link PatcherBuilder} object used to build the {@link Patcher}.
     */
    private Patcher(PatcherBuilder builder) {
        this.ctClass = builder.ctClass;
        this.className = builder.className;
    }

    /**
     * Applying modifications to the class being modified.
     * After applying the changes, re-modification will occur from the original class.
     */
    public void applyPatch() {
        try {
            AgentManager.modifyClass(this.className, this.ctClass.toBytecode());
        } catch (Exception e) {
            System.out.println("[!] Error when trying to apply a modification: " + e.getMessage());
        }
    }

    /**
     * A class that implements the Builder pattern to create a {@link Patcher} object
     */
    public static class PatcherBuilder {
        private final String className; // Path to class
        private CtClass ctClass; // Represents a modifiable class

        /**
         * Builder constructor.
         *
         * @param className the name of the class that needs to be modified
         */
        public PatcherBuilder(String className) {
            this.className = className;
        }

        /**
         * Gets the {@link CtClass} object for the specified class name.
         *
         * @param className class name
         * @return the {@link CtClass} object
         * @throws NotFoundException if the class is not found
         */
        private CtClass getCtClass(String className) throws NotFoundException {
            ClassPool pool = ClassPool.getDefault();
            return this.ctClass == null ? pool.get(className) : this.ctClass;
        }

        /**
         * Applies a patch to the specified class method.
         *
         * @param methodName     method name
         * @param methodModifier function that modifies the method
         * @return the {@link PatcherBuilder} instance for the call chain
         */
        public PatcherBuilder patchMethod(String methodName, BiConsumer<CtClass, CtMethod> methodModifier) {
            System.out.printf("[#] Attempt to patch a class '%s' in method: '%s'%n", className, methodName);

            try {
                CtClass modifyClass = getCtClass(className);
                CtMethod ctMethod = modifyClass.getDeclaredMethod(methodName);

                if (modifyClass.isFrozen()) {
                    modifyClass.defrost();
                }

                methodModifier.accept(modifyClass, ctMethod);
                modifyClass.detach();

                this.ctClass = modifyClass;
            } catch (Exception e) {
                System.out.printf("[!] An error occurred while patching class '%s'!%n", className);
                e.printStackTrace();
            }

            return this;
        }

        /**
         * Applies a patch to the specified class method.
         *
         * @param className       name of the class that contains the fix method
         * @param methodSignature the method signature in a format suitable for searching for method overloads (e.g. "int, String")
         * @param methodName      method name
         * @param methodModifier  function that modifies the method
         * @return the {@link PatcherBuilder} instance for the call chain
         */
        public PatcherBuilder patchMethod(String className, String methodName, String methodSignature, BiConsumer<CtClass, CtMethod> methodModifier) {
            String[] argsTypes = methodSignature.split(",");
            System.out.printf("[#] Attempt to patch a class '%s' in method: '%s(%s args)%n", className, methodName, argsTypes.length);

            try {
                CtClass modifyClass = getCtClass(className);
                CtMethod ctMethod = modifyClass.getDeclaredMethod(methodName, getMethodParameterTypes(modifyClass.getClassPool(), methodSignature));

                if (modifyClass.isFrozen()) {
                    modifyClass.defrost();
                }

                methodModifier.accept(modifyClass, ctMethod);
                modifyClass.detach();

                this.ctClass = modifyClass;
            } catch (Exception e) {
                System.out.printf("[!] An error occurred while patching class '%s'!%n", className);
                e.printStackTrace();
            }
            return this;
        }

        /**
         * Retrieving the types of method parameters based on its signature.
         *
         * @param classPool       the class pool used to load classes
         * @param methodSignature the method signature in a format containing the parameter types (e.g. "int, String")
         * @return an array of {@link CtClass} objects representing the method parameter types
         * @throws NotFoundException if any of the parameter types cannot be found in the class pool
         */
        private CtClass[] getMethodParameterTypes(ClassPool classPool, String methodSignature) throws NotFoundException {
            String[] argsType = methodSignature.split(",");
            CtClass[] parameterTypes = new CtClass[argsType.length];
            for (int i = 0; i < argsType.length; i++) {
                parameterTypes[i] = classPool.get(argsType[i].trim());
            }
            return parameterTypes;
        }

        /**
         * Creates a {@link Patcher} object based on the current state of the builder.
         *
         * @return {@link Patcher} object
         */
        public Patcher build() {
            return new Patcher(this);
        }
    }
}