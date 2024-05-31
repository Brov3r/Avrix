package com.avrix.agent;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.BiConsumer;

/**
 * A class for modifying Java classes at runtime using the <a href="https://www.javassist.org/">Javassist library</a>.
 */
public class ClassModifier {
    private final CtClass ctClass; // Represents a modifiable class
    private final String className; // Path to class

    /**
     * Constructor for creating a {@link ClassModifier} object.
     *
     * @param builder the {@link ClassModifierBuilder} object used to build the {@link ClassModifier}.
     */
    private ClassModifier(ClassModifierBuilder builder) {
        this.ctClass = builder.ctClass;
        this.className = builder.className;
    }

    /**
     * Applying modifications to the class being modified.
     */
    public void applyModifications() {
        try {
            AgentManager.transformClass(this.className, this.ctClass.toBytecode());
        } catch (Exception e) {
            System.out.println("[!] Error when trying to apply a modification: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * A class that implements the Builder pattern to create a {@link ClassModifier} object
     */
    public static class ClassModifierBuilder {
        private final String className; // Path to class
        private CtClass ctClass; // Represents a modifiable class

        /**
         * Builder constructor for class {@link ClassModifier}
         *
         * @param className the name of the class that needs to be modified
         */
        public ClassModifierBuilder(String className) {
            this.className = className;
        }

        /**
         * Gets the {@link CtClass} object for the specified class name.
         *
         * @param className class name
         * @return the {@link CtClass} object
         * @throws NotFoundException if the class is not found
         */
        private synchronized CtClass getCtClass(String className) throws NotFoundException {
            return this.ctClass == null ? ClassPool.getDefault().get(className) : this.ctClass;
        }

        /**
         * Applying modifications to a method by its name using a set of instructions.
         *
         * @param methodName     name of the method being modified
         * @param methodModifier set of instructions for changing a method
         * @return the {@link ClassModifierBuilder} instance for the call chain
         */
        public ClassModifierBuilder modifyMethod(String methodName, BiConsumer<CtClass, CtMethod> methodModifier) {
            return modifyMethod(methodName, null, methodModifier);
        }

        /**
         * Applying modifications to a method by its name using a set of instructions.
         *
         * @param methodSignature the method signature in a format suitable for searching for method overloads (e.g. "int, String")
         * @param methodName      name of the method being modified
         * @param methodModifier  set of instructions for changing a method
         * @return the {@link ClassModifierBuilder} instance for the call chain
         */
        public ClassModifierBuilder modifyMethod(String methodName, String methodSignature, BiConsumer<CtClass, CtMethod> methodModifier) {
            System.out.printf("[#] Attempt to patch a class '%s' in method: '%s'%s%n", this.className, methodName,
                    methodSignature != null ? "(" + methodSignature + " args)" : "");

            try {
                CtClass modifyClass = getCtClass(this.className);
                CtMethod ctMethod;

                if (methodSignature == null) {
                    ctMethod = modifyClass.getDeclaredMethod(methodName);
                } else {
                    ctMethod = modifyClass.getDeclaredMethod(methodName, getMethodParameterTypes(modifyClass.getClassPool(), methodSignature));
                }

                if (modifyClass.isFrozen()) {
                    modifyClass.defrost();
                }

                methodModifier.accept(modifyClass, ctMethod);
                modifyClass.detach();

                this.ctClass = modifyClass;
            } catch (Exception e) {
                System.out.printf("[!] An error occurred while patching class '%s'. Reason: %s%n", this.className, e.getMessage());
                throw new RuntimeException(e);
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
         * Creates a {@link ClassModifier} object based on the current state of the builder.
         *
         * @return {@link ClassModifier} object
         */
        public ClassModifier build() {
            return new ClassModifier(this);
        }

        /**
         * Saving a modified class to the "debug" directory.
         * This method calls {@link #saveFile(Path)} passing the "debug" path as a parameter.
         * Used to control changes to classes/methods.
         */
        public void saveFile() {
            saveFile(Paths.get("debug"));
        }

        /**
         * Saving a modified class to the specified directory.
         * Used to control changes to classes/methods.
         *
         * @param path path to the directory where the class will be saved
         */
        public void saveFile(Path path) {
            System.out.printf("[#] Saving a modified class '%s' to directory: %s%n", this.className, path);

            try {
                Files.createDirectories(path);
                ctClass.writeFile(path.toString());

                System.out.printf("[#] Class '%s' successfully saved to directory: %s%n", this.className, path);
            } catch (IOException e) {
                System.err.printf("[!] Failed to save class '%s' to directory '%s'. Reason: %s%n", this.className, path, e.getMessage());
            } catch (Exception e) {
                System.err.printf("[!] An error occurred while saving class '%s' to directory '%s'. Reason: %s%n", this.className, path, e.getMessage());
            }
        }
    }
}