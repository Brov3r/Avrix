package com.avrix.patches;

import com.avrix.agent.ClassTransformer;
import com.avrix.ui.InputWidgetHandler;
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
        getModifierBuilder().modifyMethod("onKeyPress", (ctClass, ctMethod) -> {
            try {
                ctMethod.insertBefore(InputWidgetHandler.class.getName() + ".onKeyPress($1);");
            } catch (CannotCompileException e) {
                throw new RuntimeException(e);
            }
        }).modifyMethod("onKeyRepeat", (ctClass, ctMethod) -> {
            try {
                ctMethod.insertBefore(InputWidgetHandler.class.getName() + ".onKeyRepeat($1);");
            } catch (CannotCompileException e) {
                throw new RuntimeException(e);
            }
        }).modifyMethod("onKeyRelease", (ctClass, ctMethod) -> {
            try {
                ctMethod.insertBefore(InputWidgetHandler.class.getName() + ".onKeyRelease($1);");
            } catch (CannotCompileException e) {
                throw new RuntimeException(e);
            }
        }).modifyMethod("update", (ctClass, ctMethod) -> {
            try {
                ctMethod.insertBefore("{ if (" + WidgetManager.class.getName() + ".isOverCustomUI()) return; }");
            } catch (CannotCompileException e) {
                throw new RuntimeException(e);
            }
        });
    }
}