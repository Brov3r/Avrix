package com.avrix.patches;

import com.avrix.agent.ClassTransformer;
import com.avrix.lua.LuaExposer;
import javassist.CannotCompileException;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;

/**
 * LuaManager$Exposer patcher
 */
public class PatchLuaManagerExposer extends ClassTransformer {
    /**
     * Constructor for creating a {@link ClassTransformer} object.
     */
    public PatchLuaManagerExposer() {
        super("zombie.Lua.LuaManager$Exposer");
    }

    /**
     * Method for performing class modification.
     * The implementing method must contain the logic for modifying the target class.
     */
    @Override
    public void modifyClass() {
        getModifierBuilder().modifyMethod("exposeAll", (ctClass, ctMethod) -> {
            try {
                String classCode =
                        "{" +
                                "java.util.Set exposedClasses = " + LuaExposer.class.getName() + ".getExposedClasses();" +
                                "java.util.Iterator iterator = exposedClasses.iterator();" +
                                "while(iterator.hasNext()) {" +
                                "java.lang.Class clazz = (java.lang.Class) iterator.next();" +
                                "this.setExposed(clazz);" +
                                "}" +
                                "}";
                ctMethod.insertBefore(classCode);

                ctMethod.instrument(new ExprEditor() {
                    public void edit(MethodCall m) throws CannotCompileException {
                        if (m.getMethodName().equals("exposeGlobalFunctions")) {
                            String code = "{" +
                                    "java.util.Set exposedObjects = " + LuaExposer.class.getName() + ".getExposedGlobalObjects();" +
                                    "java.util.Iterator iterator = exposedObjects.iterator();" +
                                    "while(iterator.hasNext()) {" +
                                    "java.lang.Object object = (java.lang.Object) iterator.next();" +
                                    "this.exposeGlobalFunctions(object);" +
                                    "}" +
                                    "$proceed($$);" +
                                    "}";
                            m.replace(code);
                        }
                    }
                });
            } catch (CannotCompileException e) {
                throw new RuntimeException(e);
            }
        });
    }
}