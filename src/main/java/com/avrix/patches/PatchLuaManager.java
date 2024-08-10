package com.avrix.patches;

import com.avrix.agent.ClassTransformer;
import com.avrix.events.EventManager;
import com.avrix.lua.LuaManager;
import javassist.CannotCompileException;

/**
 * LuaManager patcher
 */
public class PatchLuaManager extends ClassTransformer {
    /**
     * Constructor for creating a {@link ClassTransformer} object.
     */
    public PatchLuaManager() {
        super("zombie.Lua.LuaManager");
    }

    /**
     * Method for performing class modification.
     * The implementing method must contain the logic for modifying the target class.
     */
    @Override
    public void modifyClass() {
        getModifierBuilder().modifyMethod("LoadDirBase", "java.lang.String", (ctClass, ctMethod) -> {
            try {
                String classCode = "{" +
                        EventManager.class.getName() + ".invokeEvent(\"onLuaFilesLoaded\", new Object[]{$1});" +
                        "}";
                ctMethod.insertAfter(classCode);
            } catch (CannotCompileException e) {
                throw new RuntimeException(e);
            }
        }).modifyMethod("RunLuaInternal", (ctClass, ctMethod) -> {
            try {
                ctMethod.insertBefore(EventManager.class.getName() + ".invokeEvent(\"onLuaScriptExecute\", $args);");
                ctMethod.insertBefore("if (" + LuaManager.class.getName() + ".isLuaBlocked($1)) return null;");
            } catch (CannotCompileException e) {
                throw new RuntimeException(e);
            }
        });
    }
}