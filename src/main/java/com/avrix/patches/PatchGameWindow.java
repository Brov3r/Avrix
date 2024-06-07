package com.avrix.patches;

import com.avrix.agent.ClassTransformer;
import com.avrix.plugin.EventManager;
import com.avrix.utils.Constants;
import javassist.CannotCompileException;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;

/**
 * Game window patcher
 */
public class PatchGameWindow extends ClassTransformer {
    /**
     * Constructor for creating a {@link ClassTransformer} object.
     */
    public PatchGameWindow() {
        super("zombie.GameWindow");
    }

    /**
     * Method for performing class modification.
     * The implementing method must contain the logic for modifying the target class.
     */
    @Override
    public void modifyClass() {
        getModifierBuilder().modifyMethod("init", (ctClass, ctMethod) -> {
            try {
                ctMethod.insertAfter(EventManager.class.getName() + ".invokeEvent(\"onGameWindowInitialized\", new Object[0]);");
            } catch (CannotCompileException e) {
                throw new RuntimeException(e);
            }
        }).modifyMethod("InitDisplay", (ctClass, ctMethod) -> {
            try {
                ctMethod.instrument(new ExprEditor() {
                    public void edit(MethodCall m) throws CannotCompileException {
                        if (m.getClassName().equals("org.lwjglx.opengl.Display") && m.getMethodName().equals("setTitle")) {
                            String newTitle = String.format("\"Project Zomboid with %s (v%s)\"", Constants.AVRIX_NAME, Constants.AVRIX_VERSION);
                            m.replace("$proceed(" + newTitle + ");");
                        }
                    }
                });
            } catch (CannotCompileException e) {
                throw new RuntimeException(e);
            }
        });
    }
}