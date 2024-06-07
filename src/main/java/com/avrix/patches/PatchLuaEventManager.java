package com.avrix.patches;

import com.avrix.agent.ClassTransformer;
import com.avrix.plugin.EventManager;
import javassist.CannotCompileException;
import javassist.NotFoundException;

/**
 * Lua Event Manager patcher
 */
public class PatchLuaEventManager extends ClassTransformer {
    /**
     * Constructor for creating a {@link ClassTransformer} object.
     */
    public PatchLuaEventManager() {
        super("zombie.Lua.LuaEventManager");
    }

    /**
     * Method for performing class modification.
     * The implementing method must contain the logic for modifying the target class.
     */
    @Override
    public void modifyClass() {
        // The maximum number of arguments that the triggerEvent method can accept
        int maxArgs = 8;

        for (int argCount = 0; argCount <= maxArgs; argCount++) {
            String signature = "java.lang.String" + ", java.lang.Object".repeat(Math.max(0, argCount));
            getModifierBuilder().modifyMethod("triggerEvent", signature, (ctClass, ctMethod) -> {
                try {
                    StringBuilder code = new StringBuilder("{ ");
                    code.append("Object[] args = new Object[").append(ctMethod.getParameterTypes().length - 1).append("]; ");
                    for (int i = 2; i <= ctMethod.getParameterTypes().length; i++) {
                        code.append("args[").append(i - 2).append("] = $").append(i).append("; ");
                    }
                    code.append(EventManager.class.getName()).append(".invokeEvent($1, args); }");

                    ctMethod.insertBefore(code.toString());
                } catch (CannotCompileException | NotFoundException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }
}