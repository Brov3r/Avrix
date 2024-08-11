package com.avrix.patches;

import com.avrix.agent.ClassTransformer;
import com.avrix.ui.WidgetManager;
import javassist.CannotCompileException;

/**
 * UIManager patcher
 */
public class PatchUIManager extends ClassTransformer {
    /**
     * Constructor for creating a {@link ClassTransformer} object.
     */
    public PatchUIManager() {
        super("zombie.ui.UIManager");
    }

    /**
     * Method for performing class modification.
     * The implementing method must contain the logic for modifying the target class.
     */
    @Override
    public void modifyClass() {
        getModifierBuilder().modifyMethod("update", (ctClass, ctMethod) -> {
            try {
                ctMethod.insertBefore("{ if (" + WidgetManager.class.getName() + ".isOverCustomUI()) return; }");
            } catch (CannotCompileException e) {
                throw new RuntimeException(e);
            }
        });
    }
}