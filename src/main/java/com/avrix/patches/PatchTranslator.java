package com.avrix.patches;

import com.avrix.agent.ClassTransformer;
import com.avrix.plugin.EventManager;
import javassist.CannotCompileException;

/**
 * Translator patcher
 */
public class PatchTranslator extends ClassTransformer {
    /**
     * Constructor for creating a {@link ClassTransformer} object.
     */
    public PatchTranslator() {
        super("zombie.core.Translator");
    }

    /**
     * Method for performing class modification.
     * The implementing method must contain the logic for modifying the target class.
     */
    @Override
    public void modifyClass() {
        getModifierBuilder().modifyMethod("setLanguage", "zombie.core.Language", (ctClass, ctMethod) -> {
            try {
                String classCode = "{" +
                        EventManager.class.getName() + ".invokeEvent(\"onChangeLanguage\", new Object[]{$1});" +
                        "}";
                ctMethod.insertAfter(classCode);
            } catch (CannotCompileException e) {
                throw new RuntimeException(e);
            }
        });
    }
}