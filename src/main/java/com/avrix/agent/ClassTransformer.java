package com.avrix.agent;

/**
 * A class for transforming Java classes at runtime.
 */
public class ClassTransformer {
    private final String className; // Name of the class being modified
    private final ClassModifier.ClassModifierBuilder modifierBuilder; // Builder for ClassModifier objects

    /**
     * Constructor for creating a {@link ClassTransformer} object.
     *
     * @param className the full name of the class that needs to be modified, e.g. 'zombie.GameWindow'
     */
    public ClassTransformer(String className) {
        this.className = className;
        this.modifierBuilder = new ClassModifier.ClassModifierBuilder(className);
    }

    /**
     * Gets the {@link ClassModifier.ClassModifierBuilder} for building class modifiers.
     *
     * @return the {@link ClassModifier.ClassModifierBuilder} instance.
     */
    public ClassModifier.ClassModifierBuilder getModifierBuilder() {
        return modifierBuilder;
    }

    /**
     * Getting the full name of the class that needs to be modified.
     *
     * @return the full class name, e.g. 'zombie.GameWindow'
     */
    public String getClassName() {
        return className;
    }

    /**
     * Method for performing class modification.
     * The implementing method must contain the logic for modifying the target class.
     */
    public void modifyClass() {
    }

    /**
     * Applying modifications to the class being modified.
     * Called after the {@link #modifyClass()} method is called
     */
    public void applyModifications() {
        modifierBuilder.build().applyModifications();
    }
}