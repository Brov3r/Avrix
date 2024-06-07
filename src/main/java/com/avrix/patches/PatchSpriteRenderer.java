package com.avrix.patches;

import com.avrix.agent.ClassTransformer;
import com.avrix.plugin.EventManager;
import javassist.CannotCompileException;

/**
 * SpriteRenderer patcher
 */
public class PatchSpriteRenderer extends ClassTransformer {
    /**
     * Constructor for creating a {@link ClassTransformer} object.
     */
    public PatchSpriteRenderer() {
        super("zombie.core.SpriteRenderer");
    }

    /**
     * Method for performing class modification.
     * The implementing method must contain the logic for modifying the target class.
     */
    @Override
    public void modifyClass() {
        getModifierBuilder().modifyMethod("postRender", (ctClass, ctMethod) -> {
            try {
                ctMethod.insertAfter(EventManager.class.getName() + ".invokeEvent(\"onPostTickRenderThread\", new Object[0]);");
            } catch (CannotCompileException e) {
                throw new RuntimeException(e);
            }
        });
    }
}