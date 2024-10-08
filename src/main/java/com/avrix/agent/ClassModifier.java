package com.avrix.agent;

import javassist.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.BiConsumer;

/**
 * A class for modifying Java classes at runtime using the <a href="https://www.javassist.org/">Javassist library</a>.
 */
public final class ClassModifier {
    private final String className; // Path to class
    private CtClass ctClass; // Represents a modifiable class

    /**
     * Constructor for creating a {@link ClassModifier} object.
     *
     * @param className the name of the class that needs to be modified
     */
    private ClassModifier(String className) {
        this.className = className;
        this.ctClass = null;
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
        private final ClassModifier classModifier; // {@link ClassModifier} object

        /**
         * Builder constructor for class {@link ClassModifier}
         *
         * @param className the name of the class that needs to be modified
         */
        public ClassModifierBuilder(String className) {
            this.classModifier = new ClassModifier(className);
        }

        /**
         * Gets the {@link CtClass} object for the specified class name.
         *
         * @param className class name
         * @return the {@link CtClass} object
         * @throws NotFoundException if the class is not found
         */
        private synchronized CtClass getCtClass(String className) throws NotFoundException {
            return this.classModifier.ctClass == null ? ClassPool.getDefault().get(className) : this.classModifier.ctClass;
        }

        /**
         * Applies a modification to a given CtClass using a BiConsumer.
         *
         * @param modifier     A BiConsumer that accepts a CtClass and an object of type T to apply modifications.
         * @param modifyClass  The CtClass object to be modified.
         * @param modifyObject The object of type T used in the modification.
         * @param <T>          The type of the object to be modified.
         */
        private <T> void applyBiConsumer(BiConsumer<CtClass, T> modifier, CtClass modifyClass, T modifyObject) {
            if (modifyClass.isFrozen()) {
                modifyClass.defrost();
            }

            modifier.accept(modifyClass, modifyObject);

            modifyClass.detach();

            this.classModifier.ctClass = modifyClass;
        }

        /**
         * Modifies a constructor of the class using a specified signature and constructor modifier.
         *
         * @param descriptor          The signature of the constructor to be modified (e.g., "()V" for a no-arg constructor).
         * @param constructorModifier A BiConsumer that accepts a CtClass and CtConstructor to apply modifications.
         * @return The current instance of ClassModifierBuilder.
         */
        public ClassModifierBuilder modifyConstructor(String descriptor, BiConsumer<CtClass, CtConstructor> constructorModifier) {
            try {
                CtClass modifyClass = getCtClass(this.classModifier.className);
                CtConstructor constructor = modifyClass.getConstructor(descriptor);
                applyBiConsumer(constructorModifier, modifyClass, constructor);
            } catch (Exception e) {
                System.out.printf("[!] An error occurred while modifying constructor '%s' of class '%s'. Reason: %s%n", descriptor, this.classModifier.className, e.getMessage());
                throw new RuntimeException(e);
            }

            return this;
        }

        /**
         * Modifies a declared constructor of the class using a specified signature and constructor modifier.
         *
         * @param paramTypes          An array of CtClass objects representing the parameter types of the constructor.
         * @param constructorModifier A BiConsumer that accepts a CtClass and CtConstructor to apply modifications.
         * @return The current instance of ClassModifierBuilder.
         */
        public ClassModifierBuilder modifyDeclaredConstructor(CtClass[] paramTypes, BiConsumer<CtClass, CtConstructor> constructorModifier) {
            try {
                CtClass modifyClass = getCtClass(this.classModifier.className);
                CtConstructor constructor = modifyClass.getDeclaredConstructor(paramTypes);
                applyBiConsumer(constructorModifier, modifyClass, constructor);
            } catch (Exception e) {
                System.out.printf("[!] An error occurred while modifying declared constructor '%s' of class '%s'. Reason: %s%n", Arrays.asList(paramTypes), this.classModifier.className, e.getMessage());
                throw new RuntimeException(e);
            }

            return this;
        }

        /**
         * Modifies a declared field of the class using a specified field modifier.
         *
         * @param fieldName     The name of the field to be modified.
         * @param fieldModifier A BiConsumer that accepts a CtClass and CtField to apply modifications.
         * @return The current instance of ClassModifierBuilder.
         */
        public ClassModifierBuilder modifyDeclaredField(String fieldName, BiConsumer<CtClass, CtField> fieldModifier) {
            return modifyDeclaredField(fieldName, "", fieldModifier);
        }

        /**
         * Modifies a declared field of the class using a specified field modifier and description.
         *
         * @param fieldName     The name of the field to be modified.
         * @param descriptor    The descriptor of the field type. If empty, it will search without descriptor.
         * @param fieldModifier A BiConsumer that accepts a CtClass and CtField to apply modifications.
         * @return The current instance of ClassModifierBuilder.
         */
        public ClassModifierBuilder modifyDeclaredField(String fieldName, String descriptor, BiConsumer<CtClass, CtField> fieldModifier) {
            try {
                CtClass modifyClass = getCtClass(this.classModifier.className);
                CtField field = !descriptor.isEmpty() ? modifyClass.getDeclaredField(fieldName, descriptor) : modifyClass.getDeclaredField(fieldName);
                applyBiConsumer(fieldModifier, modifyClass, field);
            } catch (Exception e) {
                System.out.printf("[!] An error occurred while patching class '%s'. Reason: %s%n", this.classModifier.className, e.getMessage());
                throw new RuntimeException(e);
            }

            return this;
        }

        /**
         * Modifies a field of the class using a specified field modifier.
         *
         * @param fieldName     The name of the field to be modified.
         * @param fieldModifier A BiConsumer that accepts a CtClass and CtField to apply modifications.
         * @return The current instance of ClassModifierBuilder.
         */
        public ClassModifierBuilder modifyField(String fieldName, BiConsumer<CtClass, CtField> fieldModifier) {
            return modifyField(fieldName, "", fieldModifier);
        }

        /**
         * Modifies a field of the class using a specified field modifier and description.
         *
         * @param fieldName     The name of the field to be modified.
         * @param descriptor    The descriptor of the field type. If empty, it will search without descriptor.
         * @param fieldModifier A BiConsumer that accepts a CtClass and CtField to apply modifications.
         * @return The current instance of ClassModifierBuilder.
         */
        public ClassModifierBuilder modifyField(String fieldName, String descriptor, BiConsumer<CtClass, CtField> fieldModifier) {
            try {
                CtClass modifyClass = getCtClass(this.classModifier.className);
                CtField field = !descriptor.isEmpty() ? modifyClass.getField(fieldName, descriptor) : modifyClass.getField(fieldName);
                applyBiConsumer(fieldModifier, modifyClass, field);
            } catch (Exception e) {
                System.out.printf("[!] An error occurred while patching class '%s'. Reason: %s%n", this.classModifier.className, e.getMessage());
                throw new RuntimeException(e);
            }

            return this;
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
            try {
                CtClass modifyClass = getCtClass(this.classModifier.className);
                CtMethod ctMethod;

                if (methodSignature == null) {
                    ctMethod = modifyClass.getDeclaredMethod(methodName);
                } else {
                    ctMethod = modifyClass.getDeclaredMethod(methodName, getMethodParameterTypes(modifyClass.getClassPool(), methodSignature));
                }

                applyBiConsumer(methodModifier, modifyClass, ctMethod);
            } catch (Exception e) {
                System.out.printf("[!] An error occurred while patching class '%s'. Reason: %s%n", this.classModifier.className, e.getMessage());
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
            Objects.requireNonNull(this.classModifier.ctClass, "[!] You must make modifications before building the ClassModifier!");
            return this.classModifier;
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
            System.out.printf("[#] Saving a modified class '%s' to directory: %s%n", this.classModifier.className, path);

            try {
                Files.createDirectories(path);
                this.classModifier.ctClass.writeFile(path.toString());

                System.out.printf("[#] Class '%s' successfully saved to directory: %s%n", this.classModifier.className, path);
            } catch (IOException e) {
                System.err.printf("[!] Failed to save class '%s' to directory '%s'. Reason: %s%n", this.classModifier.className, path, e.getMessage());
            } catch (Exception e) {
                System.err.printf("[!] An error occurred while saving class '%s' to directory '%s'. Reason: %s%n", this.classModifier.className, path, e.getMessage());
            }
        }
    }
}